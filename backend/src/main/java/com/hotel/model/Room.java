package com.hotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomNumber;
    private String type; // SINGLE, DOUBLE, SUITE
    @Column(name = "base_price")
    private Double basePrice;

    @Column(name = "current_price")
    private Double currentPrice;

    @Column(name = "price")
    private Double price; // legacy column fallback
    private String status; // AVAILABLE, OCCUPIED, UNDER_MAINTENANCE
    private boolean available;
    private String description;
}