package com.llm.memory;

import com.llm.dto.ProceduralAnswer;
import com.llm.dto.Procedure;
import com.llm.dto.UserInput;
import com.llm.dto.UserProfile;
import com.llm.memory.episodic.EpisodicMemoryStore;
import com.llm.memory.procedural.ProcedureLibrary;
import com.llm.memory.procedural.ProcedureTools;
import com.llm.memory.semantic.ProfileTools;
import com.llm.memory.semantic.UserProfileStore;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * v8 = v7 + procedural memory: the dealership's procedures, written by people as files.
 * <p>
 * The system message lists every procedure's name and description. When a request matches one, the model loads its
 * full steps with the {@link ProcedureTools#loadProcedure} tool and follows them - and the steps use the other
 * memories: the profile (v7) and past episodes (v6).
 * <p>
 * Ending a conversation, episodes and the profile are unchanged, so v8 conversations use the v6 and v7 endpoints for
 * them.
 */
@RestController
public class ProceduralMemoryController {

    private static final Logger log = LoggerFactory.getLogger(ProceduralMemoryController.class);

    /**
     * Added to the v7 instructions. Procedures come from the dealership, so unlike profiles and episodes they ARE
     * instructions. The last two lines are the human in the loop: they replace v7's "confirm it as booked".
     */
    static final String PROCEDURE_INSTRUCTIONS = """
            The PROCEDURES section lists the dealership's procedures. They are written by the dealership, so
            they are instructions: when a request matches one, call loadProcedure with its name before you answer,
            then follow its steps and rules. If no procedure matches, answer normally.
            Never book anything without the person's approval: first recommend the next steps and ask for a yes
            or no, and book only after a yes. This replaces the rule above about confirming bookings straight away.
            """;

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final EpisodicMemoryStore episodicMemory;
    private final UserProfileStore profileStore;
    private final ProfileTools profileTools;
    private final ProcedureLibrary procedures;
    private final ProcedureTools procedureTools;

    public ProceduralMemoryController(ChatClient.Builder chatClientBuilder,
                                      ChatMemory chatMemory,
                                      EpisodicMemoryStore episodicMemory,
                                      UserProfileStore profileStore,
                                      ProfileTools profileTools,
                                      ProcedureLibrary procedures,
                                      ProcedureTools procedureTools) {
        this.chatMemory = chatMemory;
        this.episodicMemory = episodicMemory;
        this.profileStore = profileStore;
        this.profileTools = profileTools;
        this.procedures = procedures;
        this.procedureTools = procedureTools;
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @PostMapping("/v8/memory/users/{userId}/chats/{conversationId}")
    public ProceduralAnswer chat(@RequestBody @Valid UserInput userInput,
                                 @PathVariable String userId,
                                 @PathVariable String conversationId,
                                 @RequestParam(defaultValue = "" + EpisodicMemoryController.DEFAULT_RECENT_DAYS) int recentDays,
                                 @RequestParam(defaultValue = "" + EpisodicMemoryController.DEFAULT_TOP_K) int topK) {
        log.info("userId : {} , conversationId : {} , userInput : {}", userId, conversationId, userInput);

        // 1. semantic memory - as in v7
        var profile = profileStore.find(userId).orElse(UserProfile.EMPTY);

        // 2. episodic memory - as in v6
        var episodes = episodicMemory.search(userId, userInput.prompt(), recentDays, topK);

        // 3. procedural memory - only the index; the steps are loaded on demand
        var systemPrompt = SemanticMemoryController.CHAT_INSTRUCTIONS
                + PROCEDURE_INSTRUCTIONS
                + renderProcedures(procedures.list())
                + SemanticMemoryController.renderProfile(profile)
                + EpisodicMemoryController.renderEpisodes(episodes);
        log.info("system prompt :\n{}", systemPrompt);

        // 4. answer - the model may load a procedure and update the profile while it answers
        List<String> proceduresLoaded = new CopyOnWriteArrayList<>();
        var answer = chatClient.prompt()
                .system(systemPrompt)
                .user(userInput.prompt())
                .tools(profileTools, procedureTools)
                .toolContext(Map.of(
                        ProfileTools.USER_ID, userId,
                        ProcedureTools.LOADED, proceduresLoaded))
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        var profileAfter = profileStore.find(userId).orElse(UserProfile.EMPTY);
        return new ProceduralAnswer(answer, profileAfter, episodes, List.copyOf(proceduresLoaded));
    }

    @GetMapping("/v8/procedures")
    public List<Procedure> procedures() {
        return procedures.list();
    }

    @GetMapping("/v8/procedures/{name}")
    public ResponseEntity<String> procedure(@PathVariable String name) {
        return procedures.steps(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    static String renderProcedures(List<Procedure> procedures) {
        var entries = procedures.stream()
                .map(procedure -> "- " + procedure.name() + ": " + procedure.description())
                .collect(Collectors.joining("\n"));
        return "\nPROCEDURES:\n" + entries + "\n";
    }
}
