package com.hotel.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BookingResponseDto {
    private Long id;
    private Long roomId;
    private String roomNumber;
    private String roomType;
    private double price;
    private double discountPercent;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkInDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkOutDate;
    private String status;
    private String username;
    private boolean checkInConfirmedByAdmin;
    private boolean checkOutConfirmedByAdmin;
}
