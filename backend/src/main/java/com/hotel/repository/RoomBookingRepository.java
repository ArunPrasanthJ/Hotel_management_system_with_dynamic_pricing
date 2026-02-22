package com.hotel.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hotel.model.RoomBooking;

public interface RoomBookingRepository extends JpaRepository<RoomBooking, Long> {
    List<RoomBooking> findByUserUsername(String username);

    @Query("SELECT b FROM RoomBooking b WHERE b.room.id = :roomId AND b.status = 'CONFIRMED' AND b.checkInDate < :checkOut AND b.checkOutDate > :checkIn")
    List<RoomBooking> findConflictingBookings(@Param("roomId") Long roomId, @Param("checkIn") LocalDate checkIn, @Param("checkOut") LocalDate checkOut);

    @Query("SELECT COUNT(b) FROM RoomBooking b WHERE b.status = 'CONFIRMED' AND b.checkInDate <= :date AND b.checkOutDate > :date")
    long countBookingsOn(@Param("date") LocalDate date);

    // count pending bookings for a room
    long countByRoomIdAndStatus(Long roomId, String status);
}