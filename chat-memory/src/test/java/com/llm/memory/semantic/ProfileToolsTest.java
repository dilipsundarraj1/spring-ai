package com.llm.memory.semantic;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProfileToolsTest {

    private final UserProfileStore store = mock(UserProfileStore.class);
    private final ProfileTools tools = new ProfileTools(store);

    @Test
    void theUserIdComesFromTheToolContextNotFromTheModel() {
        var result = tools.updateProfile(ProfileField.CONTACT_PREFERENCE, "Text only, never call",
                new ToolContext(Map.of(ProfileTools.USER_ID, "dilip")));

        verify(store).update("dilip", ProfileField.CONTACT_PREFERENCE, "Text only, never call");
        assertThat(result).isEqualTo("Saved: CONTACT_PREFERENCE = Text only, never call");
    }
}
