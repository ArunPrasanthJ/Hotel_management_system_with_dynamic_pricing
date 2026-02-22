package com.hotel.service;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.hotel.dto.RoomAvailabilityDto;
import com.hotel.model.Room;
import com.hotel.model.RoomBooking;
import com.hotel.model.User;
import com.hotel.repository.RoomBookingRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.UserRepository;

@Service
public class BookingService {
    @Autowired
    private RoomBookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.hotel.service.PriceService priceService;

    @Autowired
    private com.hotel.service.AvailabilityPublisher availabilityPublisher;
    @Autowired
    private RoomRepository roomRepository;

    public List<RoomBooking> getAllBookings() {
        List<RoomBooking> bookings = bookingRepository.findAll();
        // Initialize the lazy collections
        bookings.forEach(booking -> {
            if (booking.getRoom() != null) {
                booking.getRoom().getRoomNumber(); // Force initialization
            }
            if (booking.getUser() != null) {
                booking.getUser().getUsername(); // Force initialization
            }
        });
        return bookings;
    }

    public RoomBooking getBookingById(Long id) {
        return bookingRepository.findById(id).orElseThrow(() ->
            new RuntimeException("Booking not found with id: " + id));
    }

    public RoomBooking createBooking(RoomBooking booking) {
        // Get the currently authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // Fetch the full user entity
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        booking.setUser(user);
        // Prevent double booking: check for conflicting confirmed bookings for the same room
        if (booking.getRoom() == null || booking.getRoom().getId() == null) {
            throw new RuntimeException("Room must be specified for booking");
        }

        Long roomId = booking.getRoom().getId();
        List<RoomBooking> conflicts = bookingRepository.findConflictingBookings(roomId, booking.getCheckInDate(), booking.getCheckOutDate());
        if (!conflicts.isEmpty()) {
            throw new RuntimeException("Room is already booked for the selected dates");
        }

        // New bookings from users should be created as PENDING and require admin confirmation.
        booking.setStatus("PENDING");
        RoomBooking saved = bookingRepository.save(booking);

        // Do NOT mark room unavailable or publish availability on user-initiated booking.
        // The admin must confirm (updateBooking) which will handle room state changes.

        return saved;
    }

    public RoomBooking updateBooking(Long id, RoomBooking bookingDetails) {
        RoomBooking booking = getBookingById(id);
        System.out.println("[BookingService] updateBooking called for id=" + id + ", existing checkIn=" + booking.getCheckInDate() + ", checkOut=" + booking.getCheckOutDate());
        System.out.println("[BookingService] incoming payload checkIn=" + bookingDetails.getCheckInDate() + ", checkOut=" + bookingDetails.getCheckOutDate() + ", status=" + bookingDetails.getStatus() + ", checkInConfirmedByAdmin=" + bookingDetails.getCheckInConfirmedByAdmin() + ", checkOutConfirmedByAdmin=" + bookingDetails.getCheckOutConfirmedByAdmin());
        if (bookingDetails.getCheckInDate() != null) {
            booking.setCheckInDate(bookingDetails.getCheckInDate());
        }
        if (bookingDetails.getCheckOutDate() != null) {
            booking.setCheckOutDate(bookingDetails.getCheckOutDate());
        }
        if (bookingDetails.getStatus() != null) {
            booking.setStatus(bookingDetails.getStatus());
        }
        // admin confirmations (may be null when called by user)
        if (bookingDetails.getCheckInConfirmedByAdmin() != null) {
            booking.setCheckInConfirmedByAdmin(bookingDetails.getCheckInConfirmedByAdmin());
        }
        if (bookingDetails.getCheckOutConfirmedByAdmin() != null) {
            booking.setCheckOutConfirmedByAdmin(bookingDetails.getCheckOutConfirmedByAdmin());
        }
        RoomBooking saved = bookingRepository.save(booking);
        System.out.println("[BookingService] saved booking id=" + saved.getId() + ", checkIn=" + saved.getCheckInDate() + ", checkOut=" + saved.getCheckOutDate() + ", status=" + saved.getStatus());
        // If status changed to CANCELLED, free room if no other conflicts
        try {
            Room room = saved.getRoom();
            if (room != null) {
                if ("CANCELLED".equalsIgnoreCase(saved.getStatus())) {
                    LocalDate today = LocalDate.now();
                    List<RoomBooking> conflicts = bookingRepository.findConflictingBookings(room.getId(), today, today.plusDays(1));
                    if (conflicts.isEmpty()) {
                        room.setAvailable(true);
                        room.setStatus("AVAILABLE");
                        roomRepository.save(room);
                    }
                } else if ("CONFIRMED".equalsIgnoreCase(saved.getStatus())) {
                    room.setAvailable(false);
                    room.setStatus("BOOKED");
                    roomRepository.save(room);
                }

                double price = priceService.computePrice(room, saved.getCheckInDate());
                RoomAvailabilityDto dto = new RoomAvailabilityDto(room.getId(), room.getRoomNumber(), room.getStatus(), room.isAvailable(), price);
                availabilityPublisher.publish(dto);
            }
        } catch (Exception ex) {
            // ignore
        }
        return saved;
    }

    // New method: update from DTO to only merge provided fields
    public RoomBooking updateBookingFromDto(Long id, com.hotel.dto.BookingUpdateDto dto) {
        RoomBooking booking = getBookingById(id);
        System.out.println("[BookingService] updateBookingFromDto for id=" + id + ", existing checkIn=" + booking.getCheckInDate() + ", checkOut=" + booking.getCheckOutDate());
        System.out.println("[BookingService] incoming DTO checkIn=" + dto.getCheckInDate() + ", checkOut=" + dto.getCheckOutDate() + ", status=" + dto.getStatus() + ", checkInConfirmedByAdmin=" + dto.getCheckInConfirmedByAdmin() + ", checkOutConfirmedByAdmin=" + dto.getCheckOutConfirmedByAdmin());

        if (dto.getCheckInDate() != null) {
            booking.setCheckInDate(dto.getCheckInDate());
        }
        if (dto.getCheckOutDate() != null) {
            booking.setCheckOutDate(dto.getCheckOutDate());
        }
        if (dto.getStatus() != null) {
            booking.setStatus(dto.getStatus());
        }
        if (dto.getCheckInConfirmedByAdmin() != null) {
            booking.setCheckInConfirmedByAdmin(dto.getCheckInConfirmedByAdmin());
        }
        if (dto.getCheckOutConfirmedByAdmin() != null) {
            booking.setCheckOutConfirmedByAdmin(dto.getCheckOutConfirmedByAdmin());
        }

        RoomBooking saved = bookingRepository.save(booking);
        System.out.println("[BookingService] saved (from DTO) booking id=" + saved.getId() + ", checkIn=" + saved.getCheckInDate() + ", checkOut=" + saved.getCheckOutDate() + ", status=" + saved.getStatus());

        // update room availability and publish availability
        try {
            Room room = saved.getRoom();
            if (room != null) {
                if ("CANCELLED".equalsIgnoreCase(saved.getStatus())) {
                    LocalDate today = LocalDate.now();
                    List<RoomBooking> conflicts = bookingRepository.findConflictingBookings(room.getId(), today, today.plusDays(1));
                    if (conflicts.isEmpty()) {
                        room.setAvailable(true);
                        room.setStatus("AVAILABLE");
                        roomRepository.save(room);
                    }
                } else if ("CONFIRMED".equalsIgnoreCase(saved.getStatus())) {
                    room.setAvailable(false);
                    room.setStatus("BOOKED");
                    roomRepository.save(room);
                }

                double price = priceService.computePrice(room, saved.getCheckInDate() != null ? saved.getCheckInDate() : LocalDate.now());
                RoomAvailabilityDto dtoOut = new RoomAvailabilityDto(room.getId(), room.getRoomNumber(), room.getStatus(), room.isAvailable(), price);
                availabilityPublisher.publish(dtoOut);
            }
        } catch (Exception ex) {
            // ignore
        }

        return saved;
    }

    public void deleteBooking(Long id) {
        RoomBooking booking = getBookingById(id);
        Long roomId = booking.getRoom() != null ? booking.getRoom().getId() : null;
        bookingRepository.delete(booking);

        // After deletion, free room if no other confirmed bookings overlap today
        try {
            if (roomId != null) {
                LocalDate today = LocalDate.now();
                List<RoomBooking> conflicts = bookingRepository.findConflictingBookings(roomId, today, today.plusDays(1));
                Room room = roomRepository.findById(roomId).orElse(null);
                if (room != null) {
                    if (conflicts.isEmpty()) {
                        room.setAvailable(true);
                        room.setStatus("AVAILABLE");
                        roomRepository.save(room);
                    }

                    double price = priceService.computePrice(room, today);
                    RoomAvailabilityDto dto = new RoomAvailabilityDto(room.getId(), room.getRoomNumber(), room.getStatus(), room.isAvailable(), price);
                    availabilityPublisher.publish(dto);
                }
            }
        } catch (Exception ex) {
            // ignore
        }
    }

    public List<RoomBooking> getMyBookings() {
        // Get the currently authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        List<RoomBooking> bookings = bookingRepository.findByUserUsername(username);
        // Initialize lazy collections
        bookings.forEach(booking -> {
            if (booking.getRoom() != null) {
                booking.getRoom().getRoomNumber(); // Force initialization
            }
            if (booking.getUser() != null) {
                booking.getUser().getUsername(); // Force initialization
            }
        });
        return bookings;
    }
}