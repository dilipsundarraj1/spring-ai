package com.llm.utils;

import com.llm.dto.flight.FlightBooking;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommonUtilTest {

    @Test
    void parseJsonToFlightBooking() {
        var jsonString = """
                ```json
                {
                    "name": "Li Wei",
                    "booking_date": "November 5, 2024",
                    "flight_info": {
                        "flight_number": "CA456",
                        "origin_airport_code": "PEK",
                        "origin_city": "Beijing",
                        "destination_airport_code": "PVG",
                        "destination_city": "Shanghai",
                        "departure_time": "10:00 AM",
                        "arrival_time": "12:30 PM"
                    },
                    "luggage": {
                        "carry_on": "1",
                        "checked_bag": "1"
                    },
                    "ticket_price": {
                        "value": "3200.00",
                        "currency": "YEN"
                    },
                    "seat_number": "22C"
                }
                ```
                """;

        var flightBooking = CommonUtils.fromJsonToType(jsonString, FlightBooking.class);
        assert flightBooking != null;
        assertEquals("CA456", flightBooking.flightInfo().flightNumber());
        assertEquals("Li Wei", flightBooking.name());
        assertEquals("November 5, 2024", flightBooking.bookingDate());
        assertEquals("22C", flightBooking.seatNumber());

    }

}