package com.llm.memory.longterm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory stand-in for the pgvector store. Reproduces the two behaviours the tests care about:
 * facts are keyed and replaced, summaries accumulate.
 */
public class FakeLongTermMemoryStore implements LongTermMemoryStore {

    private final Map<String, Map<FactKey, StoredFact>> facts = new LinkedHashMap<>();
    private final Map<String, List<TopicSummary>> summaries = new LinkedHashMap<>();

    @Override
    public void upsertFact(String userId, ProfileFact fact) {
        facts.computeIfAbsent(userId, key -> new LinkedHashMap<>())
                .put(fact.key(), new StoredFact(fact, Instant.now()));
    }

    @Override
    public void addSummary(String userId, TopicSummary summary) {
        summaries.computeIfAbsent(userId, key -> new ArrayList<>()).add(summary);
    }

    @Override
    public List<StoredFact> findFacts(String userId) {
        return List.copyOf(facts.getOrDefault(userId, Map.of()).values());
    }

    @Override
    public List<ScoredSummary> searchSummaries(String userId, String query, int topK) {
        return findAllSummaries(userId).stream().limit(topK).toList();
    }

    @Override
    public List<ScoredSummary> findAllSummaries(String userId) {
        return summaries.getOrDefault(userId, List.of()).stream()
                .map(summary -> new ScoredSummary(summary, 1.0, Instant.now()))
                .toList();
    }

    @Override
    public void deleteAll(String userId) {
        facts.remove(userId);
        summaries.remove(userId);
    }
}
