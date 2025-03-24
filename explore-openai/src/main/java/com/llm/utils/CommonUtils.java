package com.llm.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CommonUtils {

    public static String flightJson(){
        return "{\n" +
                "    \"name\": \"John Doe\",\n" +
                "    \"booking_date\": \"January 11, 2024\",\n" +
                "    \"flight_info\": {\n" +
                "        \"flight_number\": \"123\",\n" +
                "        \"origin_airport_code\": \"JFK\",\n" +
                "        \"origin_city\": \"New York\",\n" +
                "        \"destination_airport_code\": \"LAX\",\n" +
                "        \"destination_city\": \"Los Angeles\",\n" +
                "        \"departure_time\": \"8:00 AM\",\n" +
                "        \"arrival_time\": \"11:30 AM\"\n" +
                "    },\n" +
                "    \"luggage\": {\n" +
                "        \"carry_on\": \"1\",\n" +
                "        \"checked_bag\": \"1\"\n" +
                "    },\n" +
                "    \"ticket_price\": {\n" +
                "        \"value\": \"450.00\",\n" +
                "        \"currency\": \"DOLLAR\"\n" +
                "    },\n" +
                "    \"seat_number\": \"14A\"\n" +
                "}";
    }

    public static <T> T fromJsonToType(String jsonString, Class<T> clazz) {
        try {
            var cleanedJsonResponse = jsonString.replace("```json", "").replace("```", "").trim();
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(cleanedJsonResponse, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
