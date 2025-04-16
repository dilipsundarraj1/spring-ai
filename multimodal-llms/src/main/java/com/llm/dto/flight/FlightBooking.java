package com.llm.dto.flight;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public record FlightBooking(
        String name,
        String bookingDate,
        FlightInfo flightInfo,
        Luggage luggage,
        TicketPrice ticketPrice,
        String seatNumber
) {}

