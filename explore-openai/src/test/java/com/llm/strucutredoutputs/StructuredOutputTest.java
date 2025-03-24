package com.llm.strucutredoutputs;

import com.llm.dto.flight.FlightBooking;
import org.junit.jupiter.api.Test;
import org.springframework.ai.converter.BeanOutputConverter;

public class StructuredOutputTest {

    @Test
    void beanStructuredTest() {
        BeanOutputConverter<FlightBooking> structuredOutput = new BeanOutputConverter<>(FlightBooking.class);
        var jsonSchema = structuredOutput.getJsonSchema();
        System.out.println(jsonSchema);
    }
}
