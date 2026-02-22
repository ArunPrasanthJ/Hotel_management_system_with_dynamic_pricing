package com.hotel.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hotel.dto.RoomAvailabilityDto;
import com.hotel.model.Room;
import com.hotel.service.RoomService;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "http://localhost:3000")
public class RoomController {
    @Autowired
    private RoomService roomService;
    @Autowired
    private com.hotel.service.PriceService priceService;
    @Autowired
    private com.hotel.repository.RoomBookingRepository bookingRepository;
    @Autowired
    private com.hotel.service.AvailabilityPublisher availabilityPublisher;

    @GetMapping("/{id}/availability")
    public RoomAvailabilityDto getAvailability(@PathVariable Long id, @RequestParam(required = false) java.time.LocalDate date) {
        java.time.LocalDate target = date != null ? date : java.time.LocalDate.now();
        Room room = roomService.getRoomById(id);
        // pass current authenticated username if available to get personalized price (loyalty/group discounts)
        String username = null;
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
                username = auth.getName();
            }
        } catch (Exception e) {
            username = null;
        }
        double price = priceService.computePrice(room, target, username);
        return new com.hotel.dto.RoomAvailabilityDto(room.getId(), room.getRoomNumber(), room.getStatus(), room.isAvailable(), price);
    }

    @GetMapping("/availability/stream")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamAvailability() {
        return availabilityPublisher.subscribe();
    }

    @GetMapping
    public java.util.List<com.hotel.dto.RoomDto> getAllRooms() {
        java.util.List<com.hotel.model.Room> rooms = roomService.getAllRooms();
        java.time.LocalDate today = java.time.LocalDate.now();
        String username = null;
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
                username = auth.getName();
            }
        } catch (Exception e) {
            username = null;
        }
        java.util.List<com.hotel.dto.RoomDto> dtos = new java.util.ArrayList<>();
        for (com.hotel.model.Room r : rooms) {
            System.out.println("[RoomController] loaded room id=" + r.getId() + ", roomNumber=" + r.getRoomNumber() + ", basePrice=" + r.getBasePrice() + ", currentPrice=" + r.getCurrentPrice() + ", price=" + r.getPrice());
            // determine which date to compute price for: prefer the user's next upcoming booking date (so group discounts for that date are visible)
            java.time.LocalDate priceDate = today;
            try {
                if (username != null) {
                    java.util.Optional<java.time.LocalDate> opt = bookingRepository.findByUserUsername(username).stream()
                            .map(b -> b.getCheckInDate())
                            .filter(d -> d != null && !d.isBefore(today))
                            .min(java.time.LocalDate::compareTo);
                    if (opt.isPresent()) priceDate = opt.get();
                }
            } catch (Exception ex) {
                // fallback to today
                priceDate = today;
            }

            double price = priceService.computePrice(r, priceDate, username);
            System.out.println("Room price computed for room " + r.getId() + " => " + price);
            boolean hasPending = bookingRepository.countByRoomIdAndStatus(r.getId(), "PENDING") > 0;
            Integer discount = null;
            double base = (r.getBasePrice() != null && r.getBasePrice() > 0) ? r.getBasePrice() : (r.getCurrentPrice() != null && r.getCurrentPrice() > 0 ? r.getCurrentPrice() : (r.getPrice() != null ? r.getPrice() : 0));
            System.out.println("[getAllRooms] Room " + r.getId() + ": base=" + base + ", computedPrice=" + price + ", username=" + username + ", priceDate=" + priceDate);
            if (base > 0) {
                int d = (int) Math.round(((base - price) / base) * 100);
                if (d < 0) d = 0; // no negative discount
                discount = d;
            }
            System.out.println("[getAllRooms] Room " + r.getId() + ": computed discount=" + discount + "%");
            dtos.add(new com.hotel.dto.RoomDto(r.getId(), r.getRoomNumber(), r.getType(), r.getBasePrice(), r.getCurrentPrice(), r.getStatus(), r.isAvailable(), r.getDescription(), price, hasPending, discount));
        }
        return dtos;
    }

    @GetMapping("/{id}")
    public com.hotel.dto.RoomDto getRoomById(@PathVariable Long id) {
        com.hotel.model.Room r = roomService.getRoomById(id);
        System.out.println("[RoomController] getRoomById loaded room id=" + r.getId() + ", basePrice=" + r.getBasePrice() + ", currentPrice=" + r.getCurrentPrice() + ", price=" + r.getPrice());
        java.time.LocalDate today = java.time.LocalDate.now();
        String username = null;
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
                username = auth.getName();
            }
        } catch (Exception e) {
            username = null;
        }
        double price = priceService.computePrice(r, today, username);
        System.out.println("Room price computed for room (single) " + r.getId() + " => " + price);
        boolean hasPending = bookingRepository.countByRoomIdAndStatus(r.getId(), "PENDING") > 0;
        Integer discount = null;
        double base = (r.getBasePrice() != null && r.getBasePrice() > 0) ? r.getBasePrice() : (r.getCurrentPrice() != null && r.getCurrentPrice() > 0 ? r.getCurrentPrice() : (r.getPrice() != null ? r.getPrice() : 0));
        if (base > 0) {
            int d = (int) Math.round(((base - price) / base) * 100);
            if (d < 0) d = 0;
            discount = d;
        }
        return new com.hotel.dto.RoomDto(r.getId(), r.getRoomNumber(), r.getType(), r.getBasePrice(), r.getCurrentPrice(), r.getStatus(), r.isAvailable(), r.getDescription(), price, hasPending, discount);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Room createRoom(@RequestBody Room room) {
        return roomService.createRoom(room);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Room updateRoom(@PathVariable Long id, @RequestBody Room room) {
        return roomService.updateRoom(id, room);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok().build();
    }
}