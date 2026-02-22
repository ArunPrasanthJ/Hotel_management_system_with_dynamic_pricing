package com.hotel.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class BookingUpdateDto {
    private String status;
    private Boolean checkInConfirmedByAdmin;
    private Boolean checkOutConfirmedByAdmin;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkInDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkOutDate;
}
