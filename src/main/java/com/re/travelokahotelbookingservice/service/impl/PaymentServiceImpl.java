package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.config.VNPayConfig;
import com.re.travelokahotelbookingservice.dto.request.CreatePaymentRequest;
import com.re.travelokahotelbookingservice.dto.response.PaymentResponseDto;
import com.re.travelokahotelbookingservice.entity.Booking;
import com.re.travelokahotelbookingservice.entity.Payment;
import com.re.travelokahotelbookingservice.entity.enums.BookingStatus;
import com.re.travelokahotelbookingservice.entity.enums.PaymentMethod;
import com.re.travelokahotelbookingservice.entity.enums.PaymentStatus;
import com.re.travelokahotelbookingservice.exception.BusinessException;
import com.re.travelokahotelbookingservice.repository.BookingRepository;
import com.re.travelokahotelbookingservice.repository.PaymentRepository;
import com.re.travelokahotelbookingservice.service.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final VNPayConfig vnPayConfig;

    @Override
    @Transactional
    public PaymentResponseDto createPayment(CreatePaymentRequest request, String currentUsername, HttpServletRequest httpRequest) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn đặt phòng với ID: " + request.getBookingId()));

        // Kiểm tra quyền: đơn đặt phòng phải thuộc user đang đăng nhập (hoặc đối chiếu email / username)
        if (booking.getUser() != null) {
            String userEmail = booking.getUser().getEmail();
            String username = booking.getUser().getUsername();
            if (!currentUsername.equalsIgnoreCase(userEmail) && !currentUsername.equalsIgnoreCase(username)) {
                throw new BusinessException("Bạn không có quyền thanh toán đơn đặt phòng này");
            }
        }

        // Tìm hoặc khởi tạo bản ghi Payment
        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseGet(() -> Payment.builder()
                        .booking(booking)
                        .amount(booking.getTotalPrice())
                        .status(PaymentStatus.PENDING)
                        .paymentMethod(request.getPaymentMethod())
                        .build());

        // Nếu đã thanh toán thành công từ trước
        if (PaymentStatus.SUCCESS.equals(payment.getStatus())) {
            throw new BusinessException("Đơn đặt phòng này đã được thanh toán thành công");
        }

        // Kiểm tra nếu đơn đặt phòng đã bị hủy hoặc hết hạn
        if (BookingStatus.CANCELLED.equals(booking.getStatus()) || BookingStatus.EXPIRED.equals(booking.getStatus())) {
            throw new BusinessException("Đơn đặt phòng này đã bị hủy hoặc hết hạn, không thể thanh toán");
        }

        // Kiểm tra giới hạn thời gian 30 phút cho thanh toán chuyển khoản/VNPay
        if ((PaymentMethod.VNPAY.equals(request.getPaymentMethod()) || PaymentMethod.CREDIT_CARD.equals(request.getPaymentMethod()))
                && booking.getCreatedAt() != null
                && booking.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(30))) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            throw new BusinessException("Đơn đặt phòng đã hết hạn thanh toán (quá thời hạn 30 phút). Vui lòng đặt phòng lại!");
        }

        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setAmount(booking.getTotalPrice());
        payment.setStatus(PaymentStatus.PENDING);

        // Trường hợp 1: Thanh toán TIỀN MẶT tại khách sạn (CASH)
        if (PaymentMethod.CASH.equals(request.getPaymentMethod())) {
            payment.setTransactionId("CASH_" + booking.getId());
            payment.setPaymentDate(null);
            paymentRepository.save(payment);

            booking.setStatus(BookingStatus.PENDING);
            bookingRepository.save(booking);

            return PaymentResponseDto.builder()
                    .paymentId(payment.getId())
                    .bookingId(booking.getId())
                    .paymentMethod(PaymentMethod.CASH)
                    .transactionId(payment.getTransactionId())
                    .amount(payment.getAmount())
                    .status(PaymentStatus.PENDING)
                    .message("Đã chọn thanh toán tiền mặt tại khách sạn khi nhận phòng")
                    .build();
        }

        // Trường hợp 2: Thanh toán trực tuyến VNPay Sandbox
        if (PaymentMethod.VNPAY.equals(request.getPaymentMethod()) || PaymentMethod.CREDIT_CARD.equals(request.getPaymentMethod())) {
            payment.setPaymentMethod(PaymentMethod.VNPAY);
            Payment savedPayment = paymentRepository.save(payment);

            String paymentUrl = buildVNPayUrl(booking, httpRequest);

            return PaymentResponseDto.builder()
                    .paymentId(savedPayment.getId())
                    .bookingId(booking.getId())
                    .paymentMethod(PaymentMethod.VNPAY)
                    .amount(savedPayment.getAmount())
                    .status(PaymentStatus.PENDING)
                    .paymentUrl(paymentUrl)
                    .message("Khởi tạo đường dẫn thanh toán VNPay thành công")
                    .build();
        }

        throw new BusinessException("Phương thức thanh toán không được hỗ trợ: " + request.getPaymentMethod());
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "search_hotels", allEntries = true),
            @CacheEvict(value = "user_bookings", allEntries = true)
    })
    @Transactional
    public PaymentResponseDto handleVNPayCallback(Map<String, String> allParams) {
        log.info("Xử lý VNPay Callback với các tham số: {}", allParams);

        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();
            if (fieldValue != null && !fieldValue.isEmpty()) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        // Kiểm tra chữ ký bảo mật
        String signValue = VNPayConfig.hashAllFields(fields, vnPayConfig.getVnpHashSecret());
        if (!signValue.equalsIgnoreCase(vnp_SecureHash)) {
            log.error("Sai chữ ký bảo mật VNPay: mong muốn {}, nhận được {}", signValue, vnp_SecureHash);
            throw new BusinessException("Chữ ký bảo mật không hợp lệ (Invalid Checksum)");
        }

        String vnp_TxnRef = allParams.get("vnp_TxnRef");
        String vnp_ResponseCode = allParams.get("vnp_ResponseCode");
        String vnp_TransactionNo = allParams.get("vnp_TransactionNo");

        // Phân tích bookingId từ vnp_TxnRef (dạng: {bookingId}_{timestamp})
        Long bookingId;
        try {
            if (vnp_TxnRef.contains("_")) {
                bookingId = Long.parseLong(vnp_TxnRef.split("_")[0]);
            } else {
                bookingId = Long.parseLong(vnp_TxnRef);
            }
        } catch (NumberFormatException e) {
            throw new BusinessException("Mã tham chiếu đơn đặt phòng không hợp lệ: " + vnp_TxnRef);
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn đặt phòng: " + bookingId));

        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseGet(() -> Payment.builder()
                        .booking(booking)
                        .paymentMethod(PaymentMethod.VNPAY)
                        .amount(booking.getTotalPrice())
                        .build());

        // Mã phản hồi "00" đại diện cho giao dịch thành công trên VNPay
        if ("00".equals(vnp_ResponseCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(vnp_TransactionNo);
            payment.setPaymentDate(LocalDateTime.now());
            paymentRepository.save(payment);

            // Cập nhật trạng thái đơn đặt phòng sang CONFIRMED
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            log.info("Thanh toán thành công đơn đặt phòng ID: {}. Đã chuyển trạng thái sang CONFIRMED.", booking.getId());

            return PaymentResponseDto.builder()
                    .paymentId(payment.getId())
                    .bookingId(booking.getId())
                    .paymentMethod(payment.getPaymentMethod())
                    .transactionId(vnp_TransactionNo)
                    .amount(payment.getAmount())
                    .status(PaymentStatus.SUCCESS)
                    .paymentDate(payment.getPaymentDate())
                    .message("Thanh toán đơn đặt phòng thành công qua VNPay")
                    .build();
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setTransactionId(vnp_TransactionNo);
            paymentRepository.save(payment);

            log.warn("Thanh toán thất bại đơn đặt phòng ID: {}. Mã phản hồi VNPay: {}", booking.getId(), vnp_ResponseCode);

            return PaymentResponseDto.builder()
                    .paymentId(payment.getId())
                    .bookingId(booking.getId())
                    .paymentMethod(payment.getPaymentMethod())
                    .transactionId(vnp_TransactionNo)
                    .amount(payment.getAmount())
                    .status(PaymentStatus.FAILED)
                    .message("Giao dịch thanh toán không thành công. Mã phản hồi: " + vnp_ResponseCode)
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentByBookingId(Long bookingId, String currentUsername) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Chưa có thông tin thanh toán cho đơn đặt phòng ID: " + bookingId));

        return PaymentResponseDto.builder()
                .paymentId(payment.getId())
                .bookingId(payment.getBooking().getId())
                .paymentMethod(payment.getPaymentMethod())
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentDate(payment.getPaymentDate())
                .build();
    }

    /**
     * Xây dựng URL thanh toán chuyển hướng sang cổng VNPay Sandbox
     */
    private String buildVNPayUrl(Booking booking, HttpServletRequest request) {
        String vnp_Version = vnPayConfig.getVnpVersion();
        String vnp_Command = vnPayConfig.getVnpCommand();
        String vnp_TmnCode = vnPayConfig.getVnpTmnCode();

        // Số tiền tính theo đơn vị VND, nhân 100 theo quy định của VNPay
        long amount = booking.getTotalPrice().multiply(BigDecimal.valueOf(100)).longValue();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        
        // Mã giao dịch độc nhất cho mỗi lượt thanh toán
        String vnp_TxnRef = booking.getId() + "_" + System.currentTimeMillis();
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don dat phong " + booking.getId());
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getVnpReturnUrl());
        vnp_Params.put("vnp_IpAddr", getClientIp(request));

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        // Hạn thanh toán 15 phút
        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                try {
                    // Xây dựng dữ liệu băm
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    // Xây dựng chuỗi query URL
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                } catch (Exception e) {
                    log.error("Lỗi mã hóa tham số VNPay: {}", e.getMessage());
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(vnPayConfig.getVnpHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return vnPayConfig.getVnpPayUrl() + "?" + queryUrl;
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return (ip != null && ip.contains(",")) ? ip.split(",")[0].trim() : (ip != null ? ip : "127.0.0.1");
    }
}
