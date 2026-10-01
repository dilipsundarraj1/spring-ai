package com.llm.memory;

import com.llm.RecordingChatModel;
import com.llm.dto.UserInput;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;

import static org.assertj.core.api.Assertions.assertThat;

class ChatModelMemoryControllerTest {

    private final RecordingChatModel chatModel = new RecordingChatModel();

    private final ChatModelMemoryController controller = new ChatModelMemoryController(chatModel,
            MessageWindowChatMemory.builder().build());

    @Test
    void manuallyManagedMemoryIsSentToTheModel() {
        var firstReply = controller.chat(new UserInput("My name is James Bond"), "007");
        controller.chat(new UserInput("What is my name?"), "007");

        assertThat(firstReply).isEqualTo("reply-1");
        assertThat(chatModel.lastPromptMessages()).containsExactly(
                "USER: My name is James Bond",
                "ASSISTANT: reply-1",
                "USER: What is my name?");
    }

    @Test
    void conversationsAreIsolatedByConversationId() {
        controller.chat(new UserInput("My name is James Bond"), "007");
        controller.chat(new UserInput("What is my name?"), "008");

        assertThat(chatModel.lastPromptMessages()).containsExactly("USER: What is my name?");
    }
}
