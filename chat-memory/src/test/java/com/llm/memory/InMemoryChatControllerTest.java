package com.llm.memory;

import com.llm.RecordingChatModel;
import com.llm.dto.ChatMessage;
import com.llm.dto.UserInput;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class InMemoryChatControllerTest {

    private final RecordingChatModel chatModel = new RecordingChatModel();

    private final InMemoryChatController controller = new InMemoryChatController(ChatClient.builder(chatModel));

    @Test
    void previousMessagesAreSentToTheModel() {
        controller.chat(new UserInput("My name is Dilip"), "user-1");
        controller.chat(new UserInput("What is my name?"), "user-1");

        assertThat(chatModel.lastPromptMessages()).containsExactly(
                "USER: My name is Dilip",
                "ASSISTANT: reply-1",
                "USER: What is my name?");
    }

    @Test
    void conversationsAreIsolatedByConversationId() {
        controller.chat(new UserInput("My name is Dilip"), "user-1");
        controller.chat(new UserInput("What is my name?"), "user-2");

        assertThat(chatModel.lastPromptMessages()).containsExactly("USER: What is my name?");
    }

    @Test
    void historyReturnsTheStoredConversation() {
        controller.chat(new UserInput("My name is Dilip"), "user-1");

        assertThat(controller.history("user-1")).containsExactly(
                new ChatMessage("USER", "My name is Dilip"),
                new ChatMessage("ASSISTANT", "reply-1"));
    }

    @Test
    void windowEvictsTheOldestTurnsOnceMaxMessagesIsExceeded() {
        // each turn stores 2 messages (user + assistant), so 6 turns = 12 messages > MAX_MESSAGES (10)
        for (int turn = 1; turn <= 6; turn++) {
            controller.chat(new UserInput("message " + turn), "user-1");
        }

        var history = controller.history("user-1");
        assertThat(history).hasSize(InMemoryChatController.MAX_MESSAGES);
        assertThat(history.getFirst()).isEqualTo(new ChatMessage("USER", "message 2"));
        assertThat(history).doesNotContain(new ChatMessage("USER", "message 1"));
    }

    @Test
    void conversationIdIsRequiredInSpringAi2() {
        var chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
                .build();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> chatClient.prompt().user("What is my name?").call().content())
                .withMessage("conversationId cannot be null");
    }

    @Test
    void clearRemovesTheConversation() {
        controller.chat(new UserInput("My name is Dilip"), "user-1");

        controller.clear("user-1");

        assertThat(controller.history("user-1")).isEmpty();
    }
}
