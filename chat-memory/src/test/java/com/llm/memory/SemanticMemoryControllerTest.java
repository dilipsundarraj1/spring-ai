package com.llm.memory;

import com.llm.RecordingChatModel;
import com.llm.dto.Episode;
import com.llm.dto.UserInput;
import com.llm.dto.UserProfile;
import com.llm.memory.episodic.EpisodicMemoryStore;
import com.llm.memory.semantic.ProfileTools;
import com.llm.memory.semantic.UserProfileStore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SemanticMemoryControllerTest {

    private final RecordingChatModel chatModel = new RecordingChatModel();
    private final ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
    private final EpisodicMemoryStore episodicMemory = mock(EpisodicMemoryStore.class);
    private final UserProfileStore profileStore = mock(UserProfileStore.class);

    private final SemanticMemoryController controller = new SemanticMemoryController(
            ChatClient.builder(chatModel), chatMemory, episodicMemory, profileStore, new ProfileTools(profileStore));

    @Test
    void theProfileIsSentInTheSystemMessage() {
        when(profileStore.find("dilip")).thenReturn(Optional.of(
                new UserProfile("Dilip", "Honda Civic", "Plano", "Text only, never call")));

        controller.chat(new UserInput("Book an oil change for Saturday morning."), "dilip", "conv-2", 30, 3);

        assertThat(chatModel.lastPromptMessages().getFirst())
                .startsWith("SYSTEM: ")
                .contains("PROFILE:\n- name: Dilip\n- vehicle: Honda Civic\n- home branch: Plano\n"
                        + "- contact preference: Text only, never call");
    }

    @Test
    void anUnknownPersonGetsAnEmptyProfile() {
        when(profileStore.find("dilip")).thenReturn(Optional.empty());

        var answer = controller.chat(new UserInput("Hi"), "dilip", "conv-1", 30, 3);

        assertThat(chatModel.lastPromptMessages().getFirst()).contains("PROFILE: (empty)");
        assertThat(answer.profile()).isEqualTo(UserProfile.EMPTY);
    }

    @Test
    void episodesAreRecalledAsInV6() {
        when(profileStore.find("dilip")).thenReturn(Optional.empty());
        when(episodicMemory.search(eq("dilip"), anyString(), anyInt(), anyInt())).thenReturn(List.of(
                new Episode("conv-1", "2026-10-09", "Dilip reported brake noise.", 0.82)));

        var answer = controller.chat(new UserInput("What about the brake noise?"), "dilip", "conv-2", 30, 3);

        assertThat(answer.recalledEpisodes()).hasSize(1);
        assertThat(chatModel.lastPromptMessages().getFirst()).contains("- 2026-10-09: Dilip reported brake noise.");
    }

    @Test
    void theModelIsOfferedTheUpdateProfileTool() {
        when(profileStore.find("dilip")).thenReturn(Optional.empty());

        controller.chat(new UserInput("Please text me, never call."), "dilip", "conv-1", 30, 3);

        var options = (ToolCallingChatOptions) chatModel.lastPrompt().getOptions();
        assertThat(options.getToolCallbacks())
                .extracting(callback -> callback.getToolDefinition().name())
                .containsExactly("updateProfile");
        assertThat(options.getToolContext()).containsEntry(ProfileTools.USER_ID, "dilip");
    }

    @Test
    void theResponseShowsTheProfileAfterTheCall() {
        // what the tool would have saved while the model answered
        when(profileStore.find("dilip")).thenReturn(Optional.empty(),
                Optional.of(new UserProfile(null, null, null, "Text only, never call")));

        var answer = controller.chat(new UserInput("Please text me, never call."), "dilip", "conv-1", 30, 3);

        assertThat(answer.profile().contactPreference()).isEqualTo("Text only, never call");
    }
}
