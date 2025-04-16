package com.llm.dto.flight;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public record FlightInfo(
        String flightNumber,
        String originAirportCode,
        String originCity,
        String destinationAirportCode,
        String destinationCity,
        String departureTime,
        String arrivalTime
) {
}
