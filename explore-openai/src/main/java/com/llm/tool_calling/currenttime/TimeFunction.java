package com.llm.tool_calling.currenttime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;


@Component
@Description("Get the current time of the city based on the weather.")
public class TimeFunction implements Function<TimeInput, TimeOutput> {
    private static final Logger log = LoggerFactory.getLogger(TimeFunction.class);

    @Override
    public TimeOutput apply(TimeInput timeInput) {
        log.info("timeInput: {} ", timeInput);
        try {
            // Convert city name to time zone
            ZoneId zoneId = ZoneId.of(timeInput.timeZone());
            ZonedDateTime zonedDateTime = ZonedDateTime.now(zoneId);

            // Format the time
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
            return new TimeOutput("The current time is " + zonedDateTime.format(formatter));
        } catch (Exception e) {
            return new TimeOutput("Invalid city name for the passed in prompt");
        }
    }

}
