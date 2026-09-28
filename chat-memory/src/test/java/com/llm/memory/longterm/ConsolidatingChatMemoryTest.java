package com.llm.memory.longterm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The mechanics of Consolidation, with no LLM and no database: the extractor and the store are
 * doubles, so every assertion here is deterministic.
 */
class ConsolidatingChatMemoryTest {

    private static final String USER = "dilip";
    private static final String SCOPE = ConsolidatingChatMemory.scopeOf(USER, "conv-a");

    private RecordingMemoryExtractor extractor;
    private FakeLongTermMemoryStore store;
    private ConsolidatingChatMemory memory;

    @BeforeEach
    void setUp() {
        extractor = new RecordingMemoryExtractor();
        store = new FakeLongTermMemoryStore();
        memory = new ConsolidatingChatMemory(new ConsolidationService(extractor, store));
    }

    @Test
    void consolidationDoesNotRunUntilTheWindowWouldOverflow() {
        addTurns(3);   // 6 messages - exactly WINDOW_SIZE

        assertThat(memory.get(SCOPE)).hasSize(ConsolidatingChatMemory.WINDOW_SIZE);
        assertThat(extractor.callCount()).isZero();
    }

    @Test
    void consolidationRunsOnTheTurnThatWouldExceedTheWindow() {
        addTurns(4);   // 8 messages - 2 over

        assertThat(extractor.callCount()).isEqualTo(1);
        assertThat(memory.get(SCOPE)).hasSize(ConsolidatingChatMemory.WINDOW_SIZE);
    }

    @Test
    void anEvictedBatchNeverSplitsATurn() {
        addTurns(4);

        // the batch handed to Consolidation starts at a USER message and pairs it with its reply
        assertThat(extractor.lastBatchRendered()).containsExactly("USER: user-1", "ASSISTANT: assistant-1");
    }

    @Test
    void aCorrectionReplacesTheFactRatherThanAccumulating() {
        extractor.respondWith(new ConsolidationResult(
                List.of(new ProfileFact(FactKey.VEHICLE, "Dilip drives a blue Tesla")), List.of()));
        addTurns(4);

        extractor.respondWith(new ConsolidationResult(
                List.of(new ProfileFact(FactKey.VEHICLE, "Dilip drives a Honda")), List.of()));
        addTurns(2);

        assertThat(store.findFacts(USER))
                .singleElement()
                .extracting(LongTermMemoryStore.StoredFact::fact)
                .isEqualTo(new ProfileFact(FactKey.VEHICLE, "Dilip drives a Honda"));
    }

    @Test
    void summariesOnDifferentSubjectsBothSurvive() {
        extractor.respondWith(new ConsolidationResult(List.of(),
                List.of(new TopicSummary("caching", "Chose Redis over Caffeine"))));
        addTurns(4);

        extractor.respondWith(new ConsolidationResult(List.of(),
                List.of(new TopicSummary("deployment", "Deploys on Cloud Run"))));
        memory.add(SCOPE, turn(5));   // exactly one more eviction, so exactly one more Consolidation

        assertThat(store.findAllSummaries(USER))
                .extracting(scored -> scored.summary().subject())
                .containsExactly("caching", "deployment");
    }

    @Test
    void aBatchWorthRememberingNothingLeavesMemoryEmpty() {
        extractor.respondWith(ConsolidationResult.empty());
        addTurns(4);

        assertThat(extractor.callCount()).isEqualTo(1);
        assertThat(store.findFacts(USER)).isEmpty();
        assertThat(store.findAllSummaries(USER)).isEmpty();
    }

    @Test
    void addReportingReturnsWhatWasLearned() {
        var fact = new ProfileFact(FactKey.NAME, "The person is called Dilip");
        extractor.respondWith(new ConsolidationResult(List.of(fact), List.of()));

        addTurns(3);
        var nothingYet = memory.addReporting(SCOPE, List.of());
        assertThat(nothingYet.isEmpty()).isTrue();

        var learned = memory.addReporting(SCOPE, turn(4));
        assertThat(learned.facts()).containsExactly(fact);
    }

    @Test
    void windowsAreIsolatedPerUserAndConversation() {
        memory.add(SCOPE, turn(1));
        memory.add(ConsolidatingChatMemory.scopeOf("someone-else", "conv-a"), turn(9));

        assertThat(memory.get(SCOPE)).hasSize(2);
        assertThat(memory.get(ConsolidatingChatMemory.scopeOf("someone-else", "conv-a"))).hasSize(2);
    }

    private void addTurns(int count) {
        for (int i = 1; i <= count; i++) {
            memory.add(SCOPE, turn(i));
        }
    }

    private static List<Message> turn(int index) {
        return List.of(new UserMessage("user-" + index), new AssistantMessage("assistant-" + index));
    }
}
