package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.dto.request.BookingRequestDto;
import com.re.travelokahotelbookingservice.dto.BookingResponseDto;
import com.re.travelokahotelbookingservice.entity.Booking;
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
}
