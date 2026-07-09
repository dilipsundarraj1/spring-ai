package com.llm;

import com.llm.chats.ChatController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@Import(ChatControllerTest.MockChatClientConfig.class)
class ChatControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ChatClient chatClient;

	private final ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

	private final ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

	/**
	 * ChatController calls chatClientBuilder.build() in its constructor, so the builder
	 * must already return the ChatClient mock when the controller bean is created —
	 * stubbing it inside a test method would be too late. A @MockitoBean builder can't
	 * do that, hence the explicit test configuration.
	 */
	@TestConfiguration(proxyBeanMethods = false)
	static class MockChatClientConfig {

		@Bean
		ChatClient chatClient() {
			return mock(ChatClient.class);
		}

		@Bean
		ChatClient.Builder chatClientBuilder(ChatClient chatClient) {
			var builder = mock(ChatClient.Builder.class);
			when(builder.build()).thenReturn(chatClient);
			return builder;
		}

	}

	@BeforeEach
	void stubChatClientChain() {
		// the ChatClient bean is a context singleton; clear stubbings from earlier tests
		reset(chatClient);
		when(chatClient.prompt()).thenReturn(requestSpec);
		when(requestSpec.system(anyString())).thenReturn(requestSpec);
		when(requestSpec.user(anyString())).thenReturn(requestSpec);
		when(requestSpec.call()).thenReturn(responseSpec);
		when(responseSpec.content()).thenReturn("Here is a joke!");
	}

	@Test
	void chatReturnsTheModelAnswer() throws Exception {
		mockMvc.perform(post("/v1/chats")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"prompt\": \"Tell me a joke\"}"))
			.andExpect(status().isOk())
			.andExpect(content().string("Here is a joke!"));
	}

	@Test
	void chatV2ReturnsTheModelAnswer() throws Exception {
		mockMvc.perform(post("/v2/chats")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"prompt\": \"How do Java streams work?\"}"))
			.andExpect(status().isOk())
			.andExpect(content().string("Here is a joke!"));
	}

	@Test
	void chatRejectsABlankPrompt() throws Exception {
		mockMvc.perform(post("/v1/chats")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"prompt\": \"\"}"))
			.andExpect(status().isBadRequest());
	}

}
