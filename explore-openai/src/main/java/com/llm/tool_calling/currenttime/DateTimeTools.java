package com.llm.tool_calling.currenttime;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;

public class DateTimeTools {

    private static final Logger log = LoggerFactory.getLogger(DateTimeTools.class);

    @Tool(
            description = "Get the current date and time in the user's timezone"
//            returnDirect = true
    )
    String getCurrentDateTime() {
        log.info("DateTimeTools is invoked - getCurrentDateTime ");
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

}