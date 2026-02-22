package com.hotel.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hotel.dto.BookingResponseDto;
import com.hotel.model.RoomBooking;
import com.hotel.service.BookingService;
import com.hotel.service.PriceService;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "http://localhost:3000")
public class BookingController {
    @Autowired
    private BookingService bookingService;

    @Autowired
    private PriceService priceService;

    private BookingResponseDto toDto(RoomBooking b) {
        double price = 0.0;
        double base = 0.0;
        try {
            String username = b.getUser() != null ? b.getUser().getUsername() : null;
            if (b.getRoom() != null) {
                if (b.getCheckInDate() != null) {
                    price = priceService.computePrice(b.getRoom(), b.getCheckInDate(), username);
                } else {
                    // prefer currentPrice, then basePrice, then legacy price column
                    if (b.getRoom().getCurrentPrice() != null && b.getRoom().getCurrentPrice() > 0) {
                        price = b.getRoom().getCurrentPrice();
                    } else if (b.getRoom().getBasePrice() != null && b.getRoom().getBasePrice() > 0) {
                        price = b.getRoom().getBasePrice();
                    } else if (b.getRoom().getPrice() != null && b.getRoom().getPrice() > 0) {
                        price = b.getRoom().getPrice();
                    }
                }
            }
        } catch (Exception e) {
            // fallback
            price = b.getRoom() != null ? (b.getRoom().getCurrentPrice() != null && b.getRoom().getCurrentPrice() > 0 ? b.getRoom().getCurrentPrice() : (b.getRoom().getBasePrice() != null && b.getRoom().getBasePrice() > 0 ? b.getRoom().getBasePrice() : (b.getRoom().getPrice() != null ? b.getRoom().getPrice() : 0.0))) : 0.0;
        }

        // debug log
        System.out.println("Booking DTO price for booking " + b.getId() + " => " + price);

        String username = b.getUser() != null ? b.getUser().getUsername() : null;
        if (b.getRoom() != null) {
            if (b.getRoom().getBasePrice() != null && b.getRoom().getBasePrice() > 0) base = b.getRoom().getBasePrice();
            else if (b.getRoom().getCurrentPrice() != null && b.getRoom().getCurrentPrice() > 0) base = b.getRoom().getCurrentPrice();
            else if (b.getRoom().getPrice() != null && b.getRoom().getPrice() > 0) base = b.getRoom().getPrice();
        }

        double discountPercent = 0.0;
        try {
            if (base > 0 && price < base) {
                discountPercent = Math.round((1.0 - (price / base)) * 10000.0) / 100.0; // two decimals
            }
        } catch (Exception ex) {
            discountPercent = 0.0;
        }

        return new BookingResponseDto(
            b.getId(),
            b.getRoom() != null ? b.getRoom().getId() : null,
            b.getRoom() != null ? b.getRoom().getRoomNumber() : null,
            b.getRoom() != null ? b.getRoom().getType() : null,
            price,
            discountPercent,
            b.getCheckInDate(),
            b.getCheckOutDate(),
            b.getStatus(),
            username,
            (b.getCheckInConfirmedByAdmin() != null ? b.getCheckInConfirmedByAdmin() : false),
            (b.getCheckOutConfirmedByAdmin() != null ? b.getCheckOutConfirmedByAdmin() : false)
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<BookingResponseDto> getAllBookings() {
        List<RoomBooking> bookings = bookingService.getAllBookings();
        if (bookings == null || bookings.isEmpty()) {
            return List.of();
        }
        // filter out CANCELLED bookings from admin view
        return bookings.stream()
                .filter(b -> b.getStatus() == null || !"CANCELLED".equalsIgnoreCase(b.getStatus()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('USER')")
    public List<BookingResponseDto> getMyBookings() {
        return bookingService.getMyBookings().stream().map(this::toDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public BookingResponseDto getBookingById(@PathVariable Long id) {
        return toDto(bookingService.getBookingById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public BookingResponseDto createBooking(@RequestBody RoomBooking booking) {
        RoomBooking saved = bookingService.createBooking(booking);
        return toDto(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public BookingResponseDto updateBooking(@PathVariable Long id, @RequestBody com.hotel.dto.BookingUpdateDto bookingDto) {
        RoomBooking saved = bookingService.updateBookingFromDto(id, bookingDto);
        return toDto(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.ok().build();
    }
}