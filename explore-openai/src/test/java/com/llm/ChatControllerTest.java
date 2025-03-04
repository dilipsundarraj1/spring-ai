package com.llm;

import com.llm.chats.ChatController;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
public class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private ChatClient chatClient;

    @MockBean
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    @Test
    public void testChatEndpoint() throws Exception {
        // Mocking the behavior of chatClient'
//        MockitoAnnotations.openMocks(this);


        // Mock the behavior of chatClientBuilder
        when(chatClientBuilder.build()).thenReturn(chatClient);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("Here is a joke!");

        // Perform GET request on /chats with a custom prompt
        mockMvc.perform(get("/chats?prompt=Tell%20me%20a%20joke"))
                .andExpect(status().isOk())
                .andExpect(content().string("Here is a joke!"));
    }

    @Test
    public void testChatEndpointWithDefaultPrompt() throws Exception {
        // Mocking the behavior of chatClient with default prompt
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user("Tell me a joke")).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("Here is a joke!");

        // Perform GET request on /chats without passing a prompt, triggering the default value
        mockMvc.perform(get("/chats"))
                .andExpect(status().isOk())
                .andExpect(content().string("Here is a joke!"));
    }
}
