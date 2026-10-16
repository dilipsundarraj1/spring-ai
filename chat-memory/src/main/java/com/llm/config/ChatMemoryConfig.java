package com.llm.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * How to change the chat memory window, which Spring AI fixes at 20 messages.
 * <p>
 * The bean below is <strong>commented out on purpose</strong>: this module runs on the default 20, so there is only one
 * number to keep track of. Uncomment it to take control of the window size.
 * <p>
 * There is no property for this. {@code ChatMemoryAutoConfiguration} declares
 * {@code MessageWindowChatMemory.builder().chatMemoryRepository(repo).build()} with
 * {@code @ConditionalOnMissingBean} and no configuration class of its own, so the window size can
 * only be changed by publishing a ChatMemory bean - which is exactly what this does. The
 * auto-configured one then backs off.
 * <p>
 * <strong>This affects v2 (JDBC), v3 (ChatModel) and v6 (episodic)</strong>, the controllers that inject
 * {@code ChatMemory}. For v6 it also bounds what an episode can cover: the summary is written from the
 * messages still in the window when the conversation ends. v1 deliberately builds its own, to keep an
 * in-memory repository while the auto-configured bean is JDBC-backed.
 * <p>
 * Once enabled it is tunable without recompiling, which is useful in a lecture - drop it to 6 to make
 * eviction happen after three Turns instead of ten:
 *
 * <pre>
 * chat-memory:
 *   window:
 *     max-messages: 6
 * </pre>
 */
@Configuration
public class ChatMemoryConfig {

    /**
     * Messages, not Turns: a Turn is a user message plus the assistant's reply, so 20 messages is ten
     * exchanges. Every message in the window is re-sent to the model on every call, so this number is
     * a direct multiplier on tokens, cost and latency.
     */
    public static final int DEFAULT_MAX_MESSAGES = 20;

//    @Bean
//    ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository,
//            @Value("${chat-memory.window.max-messages:" + DEFAULT_MAX_MESSAGES + "}") int maxMessages) {
//        return MessageWindowChatMemory.builder()
//                .chatMemoryRepository(chatMemoryRepository)
//                .maxMessages(maxMessages)
//                .build();
//    }
}
