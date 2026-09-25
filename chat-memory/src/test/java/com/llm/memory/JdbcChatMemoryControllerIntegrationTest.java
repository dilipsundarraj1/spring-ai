package com.llm.memory;

import com.llm.RecordingChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the full application against an in-memory HSQLDB instead of Postgres,
 * with a fake ChatModel instead of OpenAI.
 */
@SpringBootTest(properties = {
        "spring.ai.openai.api-key=test-key",
        "spring.datasource.url=jdbc:hsqldb:mem:chat-memory",
        "spring.datasource.driver-class-name=org.hsqldb.jdbc.JDBCDriver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        // pgvector's schema init runs "CREATE EXTENSION vector", which HSQLDB cannot parse.
        // The VectorStore bean is still created, it just never touches the database here.
        "spring.ai.vectorstore.pgvector.initialize-schema=false"
})
@AutoConfigureMockMvc
@Import(JdbcChatMemoryControllerIntegrationTest.FakeChatModelConfig.class)
class JdbcChatMemoryControllerIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeChatModelConfig {

        @Bean
        @Primary
        RecordingChatModel recordingChatModel() {
            return new RecordingChatModel();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordingChatModel chatModel;

    @Autowired
    private ChatMemoryRepository chatMemoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        chatModel.reset();
        jdbcTemplate.update("DELETE FROM SPRING_AI_CHAT_MEMORY");
    }

    @Test
    void autoConfiguredRepositoryIsJdbcBacked() {
        assertThat(chatMemoryRepository).isInstanceOf(JdbcChatMemoryRepository.class);
    }

    @Test
    void conversationIsPersistedAndSentBackToTheModel() throws Exception {
        chat("jdbc-user", "My name is Dilip").andExpect(content().string("reply-1"));
        chat("jdbc-user", "What is my name?");

        assertThat(chatModel.lastPromptMessages()).containsExactly(
                "USER: My name is Dilip",
                "ASSISTANT: reply-1",
                "USER: What is my name?");

        var storedRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SPRING_AI_CHAT_MEMORY WHERE conversation_id = ?", Integer.class, "jdbc-user");
        assertThat(storedRows).isEqualTo(4);
    }

    @Test
    void historyAndConversationIdsEndpoints() throws Exception {
        chat("jdbc-user", "My name is Dilip");

        mockMvc.perform(get("/v2/memory/chats/jdbc-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("USER"))
                .andExpect(jsonPath("$[0].text").value("My name is Dilip"))
                .andExpect(jsonPath("$[1].type").value("ASSISTANT"))
                .andExpect(jsonPath("$[1].text").value("reply-1"));

        mockMvc.perform(get("/v2/memory/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("jdbc-user")));
    }

    @Test
    void clearDeletesTheConversationFromTheDatabase() throws Exception {
        chat("jdbc-user", "My name is Dilip");

        mockMvc.perform(delete("/v2/memory/chats/jdbc-user"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v2/memory/chats/jdbc-user"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void blankPromptIsRejected() throws Exception {
        mockMvc.perform(post("/v2/memory/chats/jdbc-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    private ResultActions chat(String conversationId, String prompt) throws Exception {
        return mockMvc.perform(post("/v2/memory/chats/{conversationId}", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\": \"" + prompt + "\"}"))
                .andExpect(status().isOk());
    }
}
