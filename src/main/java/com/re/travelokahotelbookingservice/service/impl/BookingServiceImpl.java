package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.dto.request.BookingRequestDto;
import com.re.travelokahotelbookingservice.dto.BookingResponseDto;
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

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import jakarta.persistence.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              RoomRepository roomRepository,
                              UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto request, String currentUsername) {

        // 1. Lấy user từ email hoặc username trong JWT
        User user = userRepository.findByEmail(currentUsername)
                .or(() -> userRepository.findByUsername(currentUsername))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUsername));

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
    public List<com.re.travelokahotelbookingservice.dto.response.AdminBookingDto> getAllBookingsAdmin() {
        return bookingRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToAdminBookingDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public com.re.travelokahotelbookingservice.dto.response.AdminBookingDto getBookingByIdAdmin(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn đặt phòng có ID: " + id));
        return mapToAdminBookingDto(booking);
    }

    @Override
    @Transactional
    public com.re.travelokahotelbookingservice.dto.response.AdminBookingDto confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn đặt phòng có ID: " + bookingId));

        if (com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CANCELLED.equals(booking.getStatus())) {
            throw new com.re.travelokahotelbookingservice.exception.BusinessException("Không thể duyệt đơn đặt phòng đã bị hủy");
        }

        booking.setStatus(com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        return mapToAdminBookingDto(booking);
    }

    @Override
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
    @Transactional
    public com.re.travelokahotelbookingservice.dto.response.AdminBookingDto updateBookingStatusAdmin(Long bookingId, String statusStr) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn đặt phòng có ID: " + bookingId));

        try {
            com.re.travelokahotelbookingservice.entity.enums.BookingStatus newStatus = 
                    com.re.travelokahotelbookingservice.entity.enums.BookingStatus.valueOf(statusStr.toUpperCase());
            booking.setStatus(newStatus);
            bookingRepository.save(booking);
            return mapToAdminBookingDto(booking);
        } catch (IllegalArgumentException e) {
            throw new com.re.travelokahotelbookingservice.exception.BusinessException("Trạng thái đơn đặt phòng không hợp lệ: " + statusStr);
        }
    }

    @Override
    public List<com.re.travelokahotelbookingservice.dto.response.AdminBookingDto> getMyBookings(String currentUsername) {
        User user = userRepository.findByEmail(currentUsername)
                .or(() -> userRepository.findByUsername(currentUsername))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUsername));

        return bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToAdminBookingDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
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

        if (com.re.travelokahotelbookingservice.entity.enums.BookingStatus.COMPLETED.equals(booking.getStatus()) ||
                com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CHECKED_IN.equals(booking.getStatus())) {
            throw new com.re.travelokahotelbookingservice.exception.BusinessException("Không thể hủy đơn đặt phòng đã nhận phòng hoặc đã hoàn thành");
        }

        booking.setStatus(com.re.travelokahotelbookingservice.entity.enums.BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        return mapToAdminBookingDto(booking);
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
                .createdAt(booking.getCreatedAt());

        if (user != null) {
            builder.userId(user.getId())
                    .customerName(user.getFullName() != null ? user.getFullName() : user.getUsername())
                    .customerEmail(user.getEmail())
                    .customerPhone(user.getPhone());
        }

        if (hotel != null) {
            builder.hotelId(hotel.getId())
                    .hotelName(hotel.getName())
                    .hotelCity(hotel.getCity())
                    .hotelAddress(hotel.getAddress())
                    .hotelThumbnailUrl(hotel.getThumbnailUrl());
        }

        if (room != null) {
            builder.roomId(room.getId())
                    .roomName(room.getName())
                    .roomType(room.getBedType() != null ? room.getBedType() : "Tiêu chuẩn")
                    .roomImageUrl(room.getImageUrl())
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
}
