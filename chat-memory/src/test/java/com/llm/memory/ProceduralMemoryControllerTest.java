package com.llm.memory;

import com.llm.RecordingChatModel;
import com.llm.dto.UserInput;
import com.llm.dto.UserProfile;
import com.llm.memory.episodic.EpisodicMemoryStore;
import com.llm.memory.procedural.ProcedureLibrary;
import com.llm.memory.procedural.ProcedureTools;
import com.llm.memory.semantic.ProfileTools;
import com.llm.memory.semantic.UserProfileStore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProceduralMemoryControllerTest {

    private final RecordingChatModel chatModel = new RecordingChatModel();
    private final UserProfileStore profileStore = mock(UserProfileStore.class);
    private final ProcedureLibrary library = new ProcedureLibrary();

    private final ProceduralMemoryController controller = new ProceduralMemoryController(
            ChatClient.builder(chatModel), MessageWindowChatMemory.builder().build(),
            mock(EpisodicMemoryStore.class), profileStore, new ProfileTools(profileStore),
            library, new ProcedureTools(library));

    ProceduralMemoryControllerTest() throws IOException {
        when(profileStore.find("dilip")).thenReturn(Optional.of(new UserProfile(null, "Honda Civic", "Plano", null)));
    }

    @Test
    void theSystemMessageListsTheProceduresButNotTheirSteps() {
        controller.chat(new UserInput("My brakes are grinding."), "dilip", "conv-1", 30, 3);

        assertThat(chatModel.lastPromptMessages().getFirst())
                .contains("PROCEDURES:\n- brake-complaint: The person reports a brake problem")
                .contains("- oil-change: ")
                .doesNotContain("Never quote a repair price")
                // the memories from v6 and v7 are still there
                .contains("PROFILE:\n- vehicle: Honda Civic")
                .contains("PAST_EPISODES: (none)")
                // the human in the loop
                .contains("Never book anything without the person's approval");
    }

    @Test
    void theModelIsOfferedBothTools() {
        controller.chat(new UserInput("My brakes are grinding."), "dilip", "conv-1", 30, 3);

        var options = (ToolCallingChatOptions) chatModel.lastPrompt().getOptions();
        assertThat(options.getToolCallbacks())
                .extracting(callback -> callback.getToolDefinition().name())
                .containsExactlyInAnyOrder("updateProfile", "loadProcedure");
        assertThat(options.getToolContext()).containsKeys(ProfileTools.USER_ID, ProcedureTools.LOADED);
    }

    @Test
    void noProcedureIsLoadedWhenTheModelDoesNotAskForOne() {
        var answer = controller.chat(new UserInput("Hi"), "dilip", "conv-1", 30, 3);

        assertThat(answer.proceduresLoaded()).isEmpty();
    }
}
