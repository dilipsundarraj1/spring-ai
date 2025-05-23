package com.llm.advisors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Custom Advisor that demonstrates creative ways to use aroundCall
 *
 * This advisor:
 * 1. Enhances requests by adding a timestamp
 * 2. Enhances responses with processing time metadata
 * 3. Tracks performance metrics
 */
//public class CustomAdvisor implements CallAdvisor {
public class CustomAdvisor {
    private static final Logger log = LoggerFactory.getLogger(CustomAdvisor.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    //    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        //        // 1. Capture start time for performance tracking
        Instant startTime = Instant.now();

        var modifiedRequest = enhanceRequest(chatClientRequest);
        log.info("Modified request: {}", modifiedRequest);
        var originalResponse =  callAdvisorChain.nextCall(chatClientRequest);
        Duration processingTime = Duration.between(startTime, Instant.now());
        return Objects.requireNonNull(enhanceResponse(originalResponse, processingTime));
    }


    private ChatClientRequest enhanceRequest(ChatClientRequest originalRequest) {
        // Get the original user messages
        List<Message> originalMessages = new ArrayList<>(originalRequest.prompt().getUserMessages());

        originalMessages.stream()
                .filter(UserMessage.class::isInstance)
                .reduce((first, second) -> second) // get the last element
                .ifPresent(lastUserMessage -> {
                    int lastIndex = originalMessages.indexOf(lastUserMessage);
                    String enhancedContent = String.format(
                            " %s \n Additional MetaData : [Request timestamp: %s]",
                            lastUserMessage.getText(),
                            TIME_FORMATTER.format(LocalDateTime.now()));
                    originalMessages.set(lastIndex, new UserMessage(enhancedContent));
                });

        // Create a new request with our enhanced messages
        var combinedMessages = new ArrayList<Message>();
        combinedMessages.add(originalRequest.prompt().getSystemMessage()); // Add systeMessage first
        combinedMessages.addAll(originalMessages); // Add user messages

        var modifiedRequest = originalRequest.mutate()
                .prompt(new Prompt(combinedMessages))
                .build();

        log.info("modifiedRequest : {} ", modifiedRequest);
        // Create a new request with our enhanced messages
        return modifiedRequest;
    }

    private ChatClientResponse enhanceResponse(ChatClientResponse chatClientResponse, Duration processingTime) {
        if (chatClientResponse.chatResponse() == null) {
            return null;
        }
        // Get the original content using the correct API method
        var chatResponse = chatClientResponse.chatResponse();
        String originalContent = chatClientResponse.chatResponse().getResult().getOutput().getText();
        if (originalContent == null) {
            return chatClientResponse;
        }

        // Add only processing time metadata to the response
        String enhancedContent = originalContent + "\n\n---\n" +
                "📊 **Response Metadata**:\n" +
                "- Processing Time: " + processingTime.toMillis() + "ms";

        var generation = new Generation(new AssistantMessage(enhancedContent));

        var newchatResponse = ChatResponse.builder()
                .generations(List.of(generation))
                .metadata(Objects.requireNonNull(chatResponse).getMetadata())
                .build();


        var enhancedResponse =  chatClientResponse.mutate()
                .chatResponse(newchatResponse)
                .build();

        log.info("Enhanced response: {}", enhancedResponse);

        return enhancedResponse;
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