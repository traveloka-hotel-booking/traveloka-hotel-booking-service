package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.dto.request.BookingRequestDto;
import com.re.travelokahotelbookingservice.dto.response.BookingResponseDto;
import com.re.travelokahotelbookingservice.entity.Booking;
import com.re.travelokahotelbookingservice.entity.Hotel;
import com.re.travelokahotelbookingservice.entity.Payment;
import com.re.travelokahotelbookingservice.entity.Room;
import com.re.travelokahotelbookingservice.entity.User;
import com.re.travelokahotelbookingservice.exception.RoomNotAvailableException;
import com.re.travelokahotelbookingservice.repository.BookingRepository;
import com.re.travelokahotelbookingservice.repository.RoomRepository;
import com.re.travelokahotelbookingservice.repository.UserRepository;
import com.re.travelokahotelbookingservice.service.BookingService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import jakarta.persistence.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final com.re.travelokahotelbookingservice.repository.PaymentRepository paymentRepository;
    private final com.re.travelokahotelbookingservice.repository.ReviewRepository reviewRepository;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              RoomRepository roomRepository,
                              UserRepository userRepository,
                              com.re.travelokahotelbookingservice.repository.PaymentRepository paymentRepository,
                              com.re.travelokahotelbookingservice.repository.ReviewRepository reviewRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "search_hotels", allEntries = true),
            @CacheEvict(value = "user_bookings", allEntries = true)
    })
    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto request, String currentUsername) {

        // 1. Lấy user từ email hoặc username trong JWT
        User user = userRepository.findByEmail(currentUsername)
                .or(() -> userRepository.findByUsername(currentUsername))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUsername));

        // Kiểm tra phân quyền: Không cho phép Admin đặt phòng
        if (com.re.travelokahotelbookingservice.entity.enums.Role.ROLE_ADMIN.equals(user.getRole())) {
            throw new com.re.travelokahotelbookingservice.exception.BusinessException(
                    "Tài khoản Quản trị viên (Admin) không được phép đặt phòng! Vui lòng sử dụng tài khoản Khách hàng (Customer).");
        }

        // 2. Lấy room
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Room not found with ID: " + request.getRoomId()));

        // 3. Kiểm tra số khách không vượt quá maxGuests
        if (request.getNumberOfGuests() > room.getMaxGuests()) {
            throw new IllegalArgumentException("Number of guests (" + request.getNumberOfGuests() 
                    + ") exceeds room capacity (" + room.getMaxGuests() + ")");
        }

        // 4. Kiểm tra phòng còn trống (JPQL chống chồng lấp theo ngày)
        List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                room.getId(),
                request.getCheckIn(),
                request.getCheckOut()
        );

        if (!overlapping.isEmpty()) {
            throw new RoomNotAvailableException("Phòng đã được đặt trong khoảng thời gian này");
        }

        // 5. Tính số đêm
        long nights = ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        if (nights <= 0) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }

        BigDecimal totalPrice = room.getPricePerNight()
                .multiply(BigDecimal.valueOf(nights));

        // 6. Tạo Booking (status = PENDING)
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setCheckIn(request.getCheckIn());
        booking.setCheckOut(request.getCheckOut());
        booking.setNumberOfGuests(request.getNumberOfGuests());
        booking.setTotalPrice(totalPrice);
        booking.setStatus(com.re.travelokahotelbookingservice.entity.enums.BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());

        bookingRepository.save(booking);

        // Lưu ý: Không cập nhật cứng room.setStatus("BOOKED") vào bảng rooms,
        // vì phòng chỉ bận trong khoảng ngày [checkIn - checkOut].
        // Tính khả dụng của phòng được quản lý tự động bởi findOverlappingBookings().

        // 7. Trả về DTO
        BookingResponseDto response = new BookingResponseDto();
        response.setBookingId(booking.getId());
        response.setTotalPrice(totalPrice);
        response.setStatus(booking.getStatus().name());

        return response;
    }

    @Override
    @Transactional
    public List<com.re.travelokahotelbookingservice.dto.response.AdminBookingDto> getAllBookingsAdmin() {
        List<Booking> bookings = bookingRepository.findAllWithDetails();
        syncExpiredBookings(bookings);
        return bookings.stream()
                .map(this::mapToAdminBookingDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public com.re.travelokahotelbookingservice.dto.response.AdminBookingDto getBookingByIdAdmin(Long id) {
        Booking booking = bookingRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn đặt phòng có ID: " + id));
        return mapToAdminBookingDto(booking);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "search_hotels", allEntries = true),
            @CacheEvict(value = "user_bookings", allEntries = true)
    })
    @Transactional
    public com.re.travelokahotelbookingservice.dto.response.AdminBookingDto confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn đặt phòng có ID: " + bookingId));

        if (com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CANCELLED.equals(booking.getStatus())) {
            throw new com.re.travelokahotelbookingservice.exception.BusinessException("Không thể duyệt đơn đặt phòng đã bị hủy");
        }

        if (com.re.travelokahotelbookingservice.entity.enums.BookingStatus.EXPIRED.equals(booking.getStatus())) {
            throw new com.re.travelokahotelbookingservice.exception.BusinessException("Không thể duyệt đơn đặt phòng đã quá hạn");
        }

        // 1. Kiểm tra ngày nhận phòng: Nếu đã qua ngày nhận phòng thì không cho duyệt
        if (booking.getCheckIn() != null && booking.getCheckIn().isBefore(LocalDate.now())) {
            booking.setStatus(com.re.travelokahotelbookingservice.entity.enums.BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            throw new com.re.travelokahotelbookingservice.exception.BusinessException(
                    "Không thể duyệt đơn: Đơn đặt phòng này đã quá hạn ngày nhận phòng (" + booking.getCheckIn() + ")! Hệ thống đã tự động chuyển sang trạng thái ĐÃ QUÁ HẠN.");
        }

        // 2. Kiểm tra thanh toán: Đối với phương thức chuyển khoản/VNPay, bắt buộc phải thanh toán thành công mới được duyệt
        Payment payment = booking.getPayment();
        if (payment != null) {
            if (com.re.travelokahotelbookingservice.entity.enums.PaymentMethod.VNPAY.equals(payment.getPaymentMethod())
                    || com.re.travelokahotelbookingservice.entity.enums.PaymentMethod.CREDIT_CARD.equals(payment.getPaymentMethod())) {
                if (!com.re.travelokahotelbookingservice.entity.enums.PaymentStatus.SUCCESS.equals(payment.getStatus())) {
                    throw new com.re.travelokahotelbookingservice.exception.BusinessException(
                            "Không thể duyệt đơn: Khách hàng chưa hoàn tất thanh toán bằng phương thức chuyển khoản/VNPay!");
                }
            }
        }

        booking.setStatus(com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        return mapToAdminBookingDto(booking);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "search_hotels", allEntries = true),
            @CacheEvict(value = "user_bookings", allEntries = true)
    })
    @Transactional
    public com.re.travelokahotelbookingservice.dto.response.AdminBookingDto cancelBookingAdmin(Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn đặt phòng có ID: " + bookingId));

        if (com.re.travelokahotelbookingservice.entity.enums.BookingStatus.COMPLETED.equals(booking.getStatus())) {
            throw new com.re.travelokahotelbookingservice.exception.BusinessException("Không thể hủy đơn đặt phòng đã hoàn thành");
        }

        booking.setStatus(com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        return mapToAdminBookingDto(booking);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "search_hotels", allEntries = true),
            @CacheEvict(value = "user_bookings", allEntries = true)
    })
    @Transactional
    public com.re.travelokahotelbookingservice.dto.response.AdminBookingDto updateBookingStatusAdmin(Long bookingId, String statusStr) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn đặt phòng có ID: " + bookingId));

        try {
            com.re.travelokahotelbookingservice.entity.enums.BookingStatus newStatus = 
                    com.re.travelokahotelbookingservice.entity.enums.BookingStatus.valueOf(statusStr.toUpperCase());

            // 1. Chặn nhận phòng nếu chưa thanh toán (Bắt buộc: Thu tiền -> Nhận phòng -> Trả phòng)
            if (com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CHECKED_IN.equals(newStatus)) {
                com.re.travelokahotelbookingservice.entity.Payment payment = booking.getPayment();
                if (payment == null || !com.re.travelokahotelbookingservice.entity.enums.PaymentStatus.SUCCESS.equals(payment.getStatus())) {
                    throw new com.re.travelokahotelbookingservice.exception.BusinessException(
                            "Không thể nhận phòng: Đơn đặt phòng chưa được thanh toán! Vui lòng thu tiền khách hàng trước khi làm thủ tục nhận phòng.");
                }
            }

            booking.setStatus(newStatus);

            // 2. Khi đơn chuyển sang COMPLETED (khách trả phòng hoàn tất lưu trú)
            if (com.re.travelokahotelbookingservice.entity.enums.BookingStatus.COMPLETED.equals(newStatus)) {
                com.re.travelokahotelbookingservice.entity.Payment payment = booking.getPayment();
                if (payment != null && com.re.travelokahotelbookingservice.entity.enums.PaymentMethod.CASH.equals(payment.getPaymentMethod())) {
                    payment.setStatus(com.re.travelokahotelbookingservice.entity.enums.PaymentStatus.SUCCESS);
                    if (payment.getPaymentDate() == null) {
                        payment.setPaymentDate(LocalDateTime.now());
                    }
                    paymentRepository.save(payment);
                }
            }

            bookingRepository.save(booking);
            return mapToAdminBookingDto(booking);
        } catch (IllegalArgumentException e) {
            throw new com.re.travelokahotelbookingservice.exception.BusinessException("Trạng thái đơn đặt phòng không hợp lệ: " + statusStr);
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "user_bookings", allEntries = true)
    })
    @Transactional
    public com.re.travelokahotelbookingservice.dto.response.AdminBookingDto payCashAdmin(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn đặt phòng có ID: " + bookingId));

        com.re.travelokahotelbookingservice.entity.Payment payment = booking.getPayment();
        if (payment == null) {
            payment = com.re.travelokahotelbookingservice.entity.Payment.builder()
                    .booking(booking)
                    .paymentMethod(com.re.travelokahotelbookingservice.entity.enums.PaymentMethod.CASH)
                    .amount(booking.getTotalPrice())
                    .build();
        }
        payment.setStatus(com.re.travelokahotelbookingservice.entity.enums.PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);
        booking.setPayment(payment);

        return mapToAdminBookingDto(booking);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "user_bookings", allEntries = true)
    })
    @Transactional
    public com.re.travelokahotelbookingservice.dto.response.AdminBookingDto checkinAdmin(Long bookingId, LocalDate actualCheckInDate) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn đặt phòng có ID: " + bookingId));

        // 1. Chặn nhận phòng nếu chưa thanh toán (Bắt buộc: Thu tiền -> Nhận phòng -> Trả phòng)
        com.re.travelokahotelbookingservice.entity.Payment payment = booking.getPayment();
        if (payment == null || !com.re.travelokahotelbookingservice.entity.enums.PaymentStatus.SUCCESS.equals(payment.getStatus())) {
            throw new com.re.travelokahotelbookingservice.exception.BusinessException(
                    "Không thể nhận phòng: Đơn đặt phòng chưa được thanh toán! Vui lòng thu tiền khách hàng trước khi làm thủ tục nhận phòng.");
        }

        if (actualCheckInDate == null) {
            actualCheckInDate = LocalDate.now();
        }

        if (booking.getCheckOut() != null && actualCheckInDate.isAfter(booking.getCheckOut())) {
            throw new com.re.travelokahotelbookingservice.exception.BusinessException(
                    "Ngày nhận phòng thực tế (" + actualCheckInDate + ") không thể sau ngày trả phòng (" + booking.getCheckOut() + ")!");
        }

        long actualNights = 1;
        if (booking.getCheckOut() != null) {
            actualNights = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(actualCheckInDate, booking.getCheckOut()));
        }

        java.math.BigDecimal pricePerNight = java.math.BigDecimal.ZERO;
        if (booking.getRoom() != null && booking.getRoom().getPricePerNight() != null) {
            pricePerNight = booking.getRoom().getPricePerNight();
        } else if (booking.getTotalPrice() != null) {
            long origNights = 1;
            if (booking.getCheckIn() != null && booking.getCheckOut() != null) {
                origNights = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut()));
            }
            pricePerNight = booking.getTotalPrice().divide(java.math.BigDecimal.valueOf(origNights), 0, java.math.RoundingMode.HALF_UP);
        }

        java.math.BigDecimal newTotal = pricePerNight.multiply(java.math.BigDecimal.valueOf(actualNights));

        booking.setCheckIn(actualCheckInDate);
        booking.setTotalPrice(newTotal);
        booking.setStatus(com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CHECKED_IN);

        if (payment != null) {
            payment.setAmount(newTotal);
            paymentRepository.save(payment);
        }

        bookingRepository.save(booking);
        return mapToAdminBookingDto(booking);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "search_hotels", allEntries = true),
            @CacheEvict(value = "user_bookings", allEntries = true)
    })
    @Transactional
    public com.re.travelokahotelbookingservice.dto.response.AdminBookingDto checkoutAdmin(Long bookingId, LocalDate actualCheckOutDate) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn đặt phòng có ID: " + bookingId));

        if (actualCheckOutDate == null) {
            actualCheckOutDate = LocalDate.now();
        }

        if (booking.getCheckIn() != null && actualCheckOutDate.isBefore(booking.getCheckIn())) {
            throw new com.re.travelokahotelbookingservice.exception.BusinessException(
                    "Ngày trả phòng thực tế (" + actualCheckOutDate + ") không thể trước ngày nhận phòng (" + booking.getCheckIn() + ")!");
        }

        long actualNights = 1;
        if (booking.getCheckIn() != null) {
            actualNights = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(booking.getCheckIn(), actualCheckOutDate));
        }

        java.math.BigDecimal pricePerNight = java.math.BigDecimal.ZERO;
        if (booking.getRoom() != null && booking.getRoom().getPricePerNight() != null) {
            pricePerNight = booking.getRoom().getPricePerNight();
        } else if (booking.getTotalPrice() != null) {
            long origNights = 1;
            if (booking.getCheckIn() != null && booking.getCheckOut() != null) {
                origNights = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut()));
            }
            pricePerNight = booking.getTotalPrice().divide(java.math.BigDecimal.valueOf(origNights), 0, java.math.RoundingMode.HALF_UP);
        }

        java.math.BigDecimal newTotal = pricePerNight.multiply(java.math.BigDecimal.valueOf(actualNights));

        booking.setCheckOut(actualCheckOutDate);
        booking.setTotalPrice(newTotal);
        booking.setStatus(com.re.travelokahotelbookingservice.entity.enums.BookingStatus.COMPLETED);

        com.re.travelokahotelbookingservice.entity.Payment payment = booking.getPayment();
        if (payment != null) {
            payment.setAmount(newTotal);
            if (com.re.travelokahotelbookingservice.entity.enums.PaymentMethod.CASH.equals(payment.getPaymentMethod())) {
                payment.setStatus(com.re.travelokahotelbookingservice.entity.enums.PaymentStatus.SUCCESS);
                if (payment.getPaymentDate() == null) {
                    payment.setPaymentDate(LocalDateTime.now());
                }
            }
            paymentRepository.save(payment);
        }

        bookingRepository.save(booking);
        return mapToAdminBookingDto(booking);
    }

    @Override
    @Cacheable(value = "user_bookings", key = "#currentUsername")
    @Transactional
    public List<com.re.travelokahotelbookingservice.dto.response.AdminBookingDto> getMyBookings(String currentUsername) {
        User user = userRepository.findByEmail(currentUsername)
                .or(() -> userRepository.findByUsername(currentUsername))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUsername));

        List<Booking> bookings = bookingRepository.findByUserIdWithDetails(user.getId());
        syncExpiredBookings(bookings);
        return bookings.stream()
                .map(this::mapToAdminBookingDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "search_hotels", allEntries = true),
            @CacheEvict(value = "user_bookings", allEntries = true)
    })
    @Transactional
    public com.re.travelokahotelbookingservice.dto.response.AdminBookingDto cancelBookingCustomer(Long bookingId, String currentUsername) {
        User user = userRepository.findByEmail(currentUsername)
                .or(() -> userRepository.findByUsername(currentUsername))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUsername));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn đặt phòng có ID: " + bookingId));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new com.re.travelokahotelbookingservice.exception.BusinessException("Bạn không có quyền thao tác trên đơn đặt phòng này");
        }

        if (com.re.travelokahotelbookingservice.entity.enums.BookingStatus.COMPLETED.equals(booking.getStatus())) {
            throw new com.re.travelokahotelbookingservice.exception.BusinessException("Không thể hủy đơn đặt phòng đã hoàn thành");
        }

        booking.setStatus(com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        return mapToAdminBookingDto(booking);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "search_hotels", allEntries = true),
            @CacheEvict(value = "user_bookings", allEntries = true)
    })
    @Transactional
    public void deleteBookingAdmin(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn đặt phòng có ID: " + id));

        if (com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CHECKED_IN.equals(booking.getStatus())) {
            // Cho phép xóa nếu đơn đã quá hạn ngày trả phòng (checkOut < today)
            if (booking.getCheckOut() == null || !booking.getCheckOut().isBefore(LocalDate.now())) {
                throw new com.re.travelokahotelbookingservice.exception.BusinessException(
                        "Không thể xóa đơn đặt phòng khi khách đang lưu trú trong thời hạn hợp lệ!");
            }
        }

        if (booking.getPayment() != null) {
            paymentRepository.delete(booking.getPayment());
        }
        bookingRepository.delete(booking);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "search_hotels", allEntries = true),
            @CacheEvict(value = "user_bookings", allEntries = true)
    })
    @Transactional
    public int cleanupExpiredBookingsAdmin() {
        LocalDate today = LocalDate.now();
        List<Booking> all = bookingRepository.findAll();
        int count = 0;
        for (Booking b : all) {
            boolean isExpired = (
                    com.re.travelokahotelbookingservice.entity.enums.BookingStatus.PENDING.equals(b.getStatus())
                    || com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CONFIRMED.equals(b.getStatus())
                    || com.re.travelokahotelbookingservice.entity.enums.BookingStatus.EXPIRED.equals(b.getStatus())
            ) && b.getCheckIn() != null && b.getCheckIn().isBefore(today);

            boolean isOverdueCheckout = com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CHECKED_IN.equals(b.getStatus())
                    && b.getCheckOut() != null && b.getCheckOut().isBefore(today);

            if (isExpired || isOverdueCheckout) {
                if (b.getPayment() != null) {
                    paymentRepository.delete(b.getPayment());
                }
                bookingRepository.delete(b);
                count++;
            }
        }
        return count;
    }

    private void syncExpiredBookings(List<Booking> bookings) {
        LocalDate today = LocalDate.now();
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(30);

        for (Booking booking : bookings) {
            com.re.travelokahotelbookingservice.entity.Payment payment = booking.getPayment();

            // 1a. Quá hạn ngày check-in: PENDING mà checkIn < today -> chuyển sang EXPIRED
            if (com.re.travelokahotelbookingservice.entity.enums.BookingStatus.PENDING.equals(booking.getStatus())
                    && booking.getCheckIn() != null
                    && booking.getCheckIn().isBefore(today)) {
                booking.setStatus(com.re.travelokahotelbookingservice.entity.enums.BookingStatus.EXPIRED);
                bookingRepository.save(booking);
            }

            // 1b. Quá hạn nhận phòng (No-Show): CONFIRMED mà checkIn < today -> chuyển sang EXPIRED
            if (com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CONFIRMED.equals(booking.getStatus())
                    && booking.getCheckIn() != null
                    && booking.getCheckIn().isBefore(today)) {
                booking.setStatus(com.re.travelokahotelbookingservice.entity.enums.BookingStatus.EXPIRED);
                bookingRepository.save(booking);
            }

            // 1c. Đơn chuyển khoản VNPay chưa thanh toán mà ngày nhận phòng đã qua hoặc quá 30 phút -> HỦY (CANCELLED)
            if (payment != null
                    && (com.re.travelokahotelbookingservice.entity.enums.PaymentMethod.VNPAY.equals(payment.getPaymentMethod())
                        || com.re.travelokahotelbookingservice.entity.enums.PaymentMethod.CREDIT_CARD.equals(payment.getPaymentMethod()))
                    && !com.re.travelokahotelbookingservice.entity.enums.PaymentStatus.SUCCESS.equals(payment.getStatus())) {
                boolean isPastCheckIn = booking.getCheckIn() != null && booking.getCheckIn().isBefore(today);
                boolean isTimeout = booking.getCreatedAt() != null && booking.getCreatedAt().isBefore(timeoutThreshold);
                if (isPastCheckIn || isTimeout) {
                    booking.setStatus(com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CANCELLED);
                    payment.setStatus(com.re.travelokahotelbookingservice.entity.enums.PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                    bookingRepository.save(booking);
                }
            }

            // 2. Tự động duyệt đơn cho Chuyển khoản/VNPay (nếu đã thanh toán thành công) HOẶC Tiền mặt (CASH)
            if (com.re.travelokahotelbookingservice.entity.enums.BookingStatus.PENDING.equals(booking.getStatus())
                    && payment != null
                    && (com.re.travelokahotelbookingservice.entity.enums.PaymentMethod.CASH.equals(payment.getPaymentMethod())
                        || ((com.re.travelokahotelbookingservice.entity.enums.PaymentMethod.VNPAY.equals(payment.getPaymentMethod())
                             || com.re.travelokahotelbookingservice.entity.enums.PaymentMethod.CREDIT_CARD.equals(payment.getPaymentMethod()))
                            && com.re.travelokahotelbookingservice.entity.enums.PaymentStatus.SUCCESS.equals(payment.getStatus())))) {
                booking.setStatus(com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CONFIRMED);
                bookingRepository.save(booking);
            }

            // 3. Đồng bộ thanh toán tiền mặt cho đơn đã Hoàn thành (COMPLETED)
            if (com.re.travelokahotelbookingservice.entity.enums.BookingStatus.COMPLETED.equals(booking.getStatus())
                    && payment != null
                    && com.re.travelokahotelbookingservice.entity.enums.PaymentMethod.CASH.equals(payment.getPaymentMethod())
                    && !com.re.travelokahotelbookingservice.entity.enums.PaymentStatus.SUCCESS.equals(payment.getStatus())) {
                payment.setStatus(com.re.travelokahotelbookingservice.entity.enums.PaymentStatus.SUCCESS);
                if (payment.getPaymentDate() == null) {
                    payment.setPaymentDate(booking.getCreatedAt() != null ? booking.getCreatedAt() : LocalDateTime.now());
                }
                paymentRepository.save(payment);
            }
        }
    }

    /**
     * Tác vụ chạy ngầm định kỳ mỗi 60 giây để đồng bộ và cập nhật đơn hết hạn / quá thời gian thanh toán.
     * Chạy độc lập trong nền giúp giải phóng luồng HTTP GET của người dùng, nâng cao hiệu năng hệ thống.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void scheduleSyncExpiredBookings() {
        try {
            List<Booking> bookings = bookingRepository.findAllWithDetails();
            syncExpiredBookings(bookings);
        } catch (Exception e) {
            log.warn("Lỗi khi chạy scheduled job scheduleSyncExpiredBookings: {}", e.getMessage());
        }
    }

    private com.re.travelokahotelbookingservice.dto.response.AdminBookingDto mapToAdminBookingDto(Booking booking) {
        if (booking == null) return null;

        User user = booking.getUser();
        Room room = booking.getRoom();
        Hotel hotel = (room != null) ? room.getHotel() : null;
        Payment payment = booking.getPayment();

        long nights = 1;
        if (booking.getCheckIn() != null && booking.getCheckOut() != null) {
            nights = Math.max(1, ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut()));
        }

        String bookingCode = String.format("BK-%04d", booking.getId());

        com.re.travelokahotelbookingservice.dto.response.AdminBookingDto.AdminBookingDtoBuilder builder = 
                com.re.travelokahotelbookingservice.dto.response.AdminBookingDto.builder()
                .id(booking.getId())
                .bookingCode(bookingCode)
                .checkIn(booking.getCheckIn())
                .checkOut(booking.getCheckOut())
                .nights(nights)
                .numberOfGuests(booking.getNumberOfGuests())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus() != null ? booking.getStatus().name() : null)
                .createdAt(booking.getCreatedAt())
                .isReviewed(reviewRepository != null && booking.getId() != null && reviewRepository.existsByBookingId(booking.getId()));

        if (user != null) {
            builder.userId(user.getId())
                    .customerName(user.getFullName() != null ? user.getFullName() : user.getUsername())
                    .customerEmail(user.getEmail())
                    .customerPhone(user.getPhone());
        }

        if (hotel != null) {
            String thumb = hotel.getThumbnailUrl();
            if ((thumb == null || thumb.isBlank()) && hotel.getImages() != null && !hotel.getImages().isBlank()) {
                thumb = extractFirstImage(hotel.getImages());
            }
            builder.hotelId(hotel.getId())
                    .hotelName(hotel.getName())
                    .hotelCity(hotel.getCity())
                    .hotelAddress(hotel.getAddress())
                    .hotelThumbnailUrl(thumb);
        }

        if (room != null) {
            String roomImg = room.getImageUrl();
            if ((roomImg == null || roomImg.isBlank()) && hotel != null) {
                roomImg = hotel.getThumbnailUrl();
                if ((roomImg == null || roomImg.isBlank()) && hotel.getImages() != null && !hotel.getImages().isBlank()) {
                    roomImg = extractFirstImage(hotel.getImages());
                }
            }
            builder.roomId(room.getId())
                    .roomName(room.getName())
                    .roomType(room.getBedType() != null ? room.getBedType() : "Tiêu chuẩn")
                    .roomImageUrl(roomImg)
                    .pricePerNight(room.getPricePerNight());
        }

        if (payment != null) {
            builder.paymentId(payment.getId())
                    .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null)
                    .paymentStatus(payment.getStatus() != null ? payment.getStatus().name() : null)
                    .transactionId(payment.getTransactionId())
                    .paymentDate(payment.getPaymentDate());
        }

        return builder.build();
    }

    private String extractFirstImage(String images) {
        if (images == null || images.isBlank()) return null;
        String trimmed = images.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            String unbracketed = trimmed.substring(1, trimmed.length() - 1).trim();
            for (String part : unbracketed.split(",")) {
                String clean = part.replace("\"", "").replace("'", "").trim();
                if (!clean.isEmpty()) return clean;
            }
        }
        for (String delimiter : new String[]{";;;", "\\|\\|\\|", "\n", ","}) {
            if (trimmed.contains(delimiter.replace("\\", ""))) {
                String[] parts = trimmed.split(delimiter);
                for (String p : parts) {
                    if (!p.trim().isEmpty()) return p.trim();
                }
            }
        }
        return trimmed;
    }
}
