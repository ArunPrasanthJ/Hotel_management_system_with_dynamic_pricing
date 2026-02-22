package com.hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoomAvailabilityDto {
    private Long roomId;
    private String roomNumber;
    private String status;
    private boolean available;
    private double price;
}
