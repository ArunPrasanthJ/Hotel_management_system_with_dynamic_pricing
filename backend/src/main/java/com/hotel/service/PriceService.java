package com.hotel.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hotel.model.Room;
import com.hotel.repository.RoomBookingRepository;
import com.hotel.repository.RoomRepository;

@Service
public class PriceService {
    @Autowired
    private RoomBookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    // Simple dynamic pricing rules:
    // - Season: months 6-8 -> +20%
    // - Demand: if bookings in next 30 days > threshold -> +10%
    // - Occupancy rate: linear increase up to +50%
    public double computePrice(Room room, LocalDate date) {
        return computePrice(room, date, null);
    }

    /**
     * Compute price and apply discounts when applicable.
     * - loyalty discount based on confirmed bookings by user
     * - site-wide discount based on total confirmed bookings on date
     */
    public double computePrice(Room room, LocalDate date, String username) {
        double base = 0.0;
        if (room.getBasePrice() != null && room.getBasePrice() > 0) {
            base = room.getBasePrice();
        } else if (room.getCurrentPrice() != null && room.getCurrentPrice() > 0) {
            base = room.getCurrentPrice();
        } else if (room.getPrice() != null && room.getPrice() > 0) {
            base = room.getPrice();
        }
        double price = base;

        // season
        int month = date.getMonthValue();
        double seasonMultiplier = (month >= 6 && month <= 8) ? 0.20 : 0.0;

        // demand: bookings in next 30 days (keeps prior behavior)
        long demand = bookingRepository.countBookingsOn(date.plusDays(1));
        double demandMultiplier = demand > 10 ? 0.10 : 0.0;

        // occupancy rate at date
        long booked = bookingRepository.countBookingsOn(date);
        long totalRooms = roomRepository.count();
        double occupancy = totalRooms > 0 ? (double) booked / (double) totalRooms : 0.0;
        double occupancyMultiplier = Math.min(0.5, occupancy * 0.5); // up to +50%

        // compute discounts
        double loyaltyDiscount = 0.0;
        try {
            if (username != null) {
                long confirmedByUser = bookingRepository.findByUserUsername(username).stream().filter(b -> "CONFIRMED".equalsIgnoreCase(b.getStatus())).count();
                if (confirmedByUser >= 10) loyaltyDiscount = 0.15;
                else if (confirmedByUser >= 5) loyaltyDiscount = 0.10;
                else if (confirmedByUser >= 1) loyaltyDiscount = 0.05;
            }
        } catch (Exception e) {
            loyaltyDiscount = 0.0;
        }

        double siteDiscount = 0.0;
        try {
            long totalConfirmed = bookingRepository.countBookingsOn(date);
            if (totalConfirmed >= 50) siteDiscount = 0.10;
            else if (totalConfirmed >= 20) siteDiscount = 0.05;
        } catch (Exception e) {
            siteDiscount = 0.0;
        }

        double totalDiscount = Math.min(0.5, loyaltyDiscount + siteDiscount);

        // group booking discount: if the same user has multiple bookings on the same date,
        // apply an additional discount based on group size (includes pending/confirmed bookings)
        double groupDiscount = 0.0;
        try {
            if (username != null && date != null) {
                long sameDayByUser = bookingRepository.findByUserUsername(username).stream()
                        .filter(b -> b.getCheckInDate() != null && b.getCheckInDate().isEqual(date))
                        .filter(b -> b.getStatus() == null || !"CANCELLED".equalsIgnoreCase(b.getStatus()))
                        .count();
                if (sameDayByUser >= 10) groupDiscount = 0.15;
                else if (sameDayByUser >= 5) groupDiscount = 0.10;
                else if (sameDayByUser >= 2) groupDiscount = 0.05;
            }
        } catch (Exception ex) {
            groupDiscount = 0.0;
        }

        totalDiscount = Math.min(0.5, totalDiscount + groupDiscount);

        // apply dynamic increases first, then discounts

        price = price * (1 + seasonMultiplier + demandMultiplier + occupancyMultiplier);
        price = price * (1.0 - totalDiscount);

        return Math.round(price * 100.0) / 100.0;
    }
}
