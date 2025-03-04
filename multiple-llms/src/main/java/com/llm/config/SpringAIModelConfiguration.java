package com.llm.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAIModelConfiguration {

    @Bean
    public ChatClient openAIChatClient(OpenAiChatModel chatModel){
        return ChatClient.create(chatModel);
    }

    @Bean
    public ChatClient ollamaAIChatClient(OllamaChatModel chatModel){
        return ChatClient.create(chatModel);
    }
}
