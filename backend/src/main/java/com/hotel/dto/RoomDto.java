package com.hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoomDto {
    private Long id;
    private String roomNumber;
    private String type;
    private Double basePrice;
    private Double currentPrice;
    private String status;
    private boolean available;
    private String description;
    private double price; // computed price for the requested date (or today)
    private boolean hasPending;
    private Integer discountPercent;
}
