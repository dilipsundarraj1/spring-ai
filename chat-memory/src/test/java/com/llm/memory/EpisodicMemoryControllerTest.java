package com.llm.memory;

import com.llm.RecordingChatModel;
import com.llm.dto.Episode;
import com.llm.dto.UserInput;
import com.llm.memory.episodic.EpisodicMemoryStore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EpisodicMemoryControllerTest {

    private final RecordingChatModel chatModel = new RecordingChatModel();
    private final ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
    private final EpisodicMemoryStore episodicMemory = mock(EpisodicMemoryStore.class);

    private final EpisodicMemoryController controller =
            new EpisodicMemoryController(ChatClient.builder(chatModel), chatMemory, episodicMemory);

    @Test
    void recalledEpisodesAreSentInTheSystemMessage() {
        when(episodicMemory.search(eq("dilip"), anyString(), anyInt(), anyInt())).thenReturn(List.of(
                new Episode("conv-1", "2026-10-09", "Dilip reported brake noise; inspection booked.", 0.82)));

        var answer = controller.chat(new UserInput("What did we decide about the brake noise?"),
                "dilip", "conv-2", 30, 3);

        assertThat(answer.recalledEpisodes()).hasSize(1);
        assertThat(chatModel.lastPromptMessages().getFirst())
                .startsWith("SYSTEM: ")
                .contains("- 2026-10-09: Dilip reported brake noise; inspection booked.");
    }

    @Test
    void endingAConversationSavesOneEpisodeAndClearsTheConversation() {
        controller.chat(new UserInput("My Civic makes a grinding noise when I brake."), "dilip", "conv-1", 30, 3);
        when(episodicMemory.save(eq("dilip"), eq("conv-1"), anyString(), any()))
                .thenReturn(new Episode("conv-1", "2026-10-09", "reply-2", null));

        var response = controller.end("dilip", "conv-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(episodicMemory).save(eq("dilip"), eq("conv-1"), eq("reply-2"), any());
        assertThat(chatMemory.get("conv-1")).isEmpty();
    }

    @Test
    void theSummaryCallSeesTheWholeTranscriptAndNoMemory() {
        controller.chat(new UserInput("My Civic makes a grinding noise when I brake."), "dilip", "conv-1", 30, 3);

        controller.end("dilip", "conv-1");

        var summaryPrompt = chatModel.lastPromptMessages();
        assertThat(summaryPrompt).hasSize(2);
        assertThat(summaryPrompt.get(0)).startsWith("SYSTEM: Summarise the conversation");
        assertThat(summaryPrompt.get(1)).isEqualTo(
                "USER: USER: My Civic makes a grinding noise when I brake.\nASSISTANT: reply-1");
    }

    @Test
    void endingAnEmptyConversationWritesNothing() {
        var response = controller.end("dilip", "never-used");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(episodicMemory, never()).save(anyString(), anyString(), anyString(), any());
    }

    @Test
    void aConversationWithNothingWorthRememberingWritesNothing() {
        ChatModel summariserSaysNone = prompt ->
                new ChatResponse(List.of(new Generation(new AssistantMessage(EpisodicMemoryController.NOTHING_TO_REMEMBER))));
        var controller = new EpisodicMemoryController(ChatClient.builder(summariserSaysNone), chatMemory, episodicMemory);
        controller.chat(new UserInput("Hi there"), "dilip", "conv-1", 30, 3);

        var response = controller.end("dilip", "conv-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(episodicMemory, never()).save(anyString(), anyString(), anyString(), any());
        assertThat(chatMemory.get("conv-1")).isEmpty();
    }
}
