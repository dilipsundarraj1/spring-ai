package com.llm.advisors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Custom Advisor that demonstrates creative ways to use aroundCall
 *
 * This advisor:
 * 1. Enhances requests by adding a timestamp
 * 2. Enhances responses with processing time metadata
 * 3. Tracks performance metrics
 */
//public class CustomAdvisor implements CallAroundAdvisor {
public class CustomAdvisor {
    private static final Logger log = LoggerFactory.getLogger(CustomAdvisor.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    //    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        // 1. Capture start time for performance tracking
        Instant startTime = Instant.now();

        // 2. Modify the request - add timestamp and metadata
        AdvisedRequest enhancedRequest = enhanceRequest(advisedRequest);
        log.info("Enhanced request: {}", enhancedRequest);

        // 3. Pass to next advisor in the chain or to the LLM
        AdvisedResponse response = chain.nextAroundCall(enhancedRequest);

        // 4. Calculate processing time
        Duration processingTime = Duration.between(startTime, Instant.now());

        // 5. Enhance the response with additional information
        return enhanceResponse(response, processingTime);
    }

    private AdvisedRequest enhanceRequest(AdvisedRequest originalRequest) {
        // Get the original user messages
        List<Message> originalMessages = new ArrayList<>(originalRequest.messages());
        // Add this to the request : "[Request timestamp: %s] %s",

        // Create a new request with our enhanced messages
        return null;
    }

    private AdvisedResponse enhanceResponse(AdvisedResponse advisedResponse, Duration processingTime) {
        if (advisedResponse == null) {
            return null;
        }
        // Get the original content using the correct API method
        var chatResponse = advisedResponse.response();
        String originalContent = advisedResponse.response().getResult().getOutput().getText();
        if (originalContent == null) {
            return advisedResponse;
        }

//        String enhancedContent = originalContent + "\n\n---\n" +
//                "📊 **Response Metadata**:\n" +
//                "- Processing Time: " + processingTime.toMillis() + "ms";
//
        return null;
    }

    //    @Override
    public String getName() {
        return "EnhancedCustomAdvisor";
    }

    //    @Override
    public int getOrder() {
        return 0;
    }
}
