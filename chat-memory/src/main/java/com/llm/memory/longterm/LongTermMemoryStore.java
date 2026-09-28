package com.llm.memory.longterm;

import java.time.Instant;
import java.util.List;

/**
 * Storage for derived memory. An interface so tests can substitute an in-memory double for the
 * pgvector implementation.
 */
public interface LongTermMemoryStore {

    /** Stores the fact, replacing any existing fact with the same key for this user. */
    void upsertFact(String userId, ProfileFact fact);

    void addSummary(String userId, TopicSummary summary);

    /** Every fact for the user. Facts are read wholesale: there are few, and each has an exact key. */
    List<StoredFact> findFacts(String userId);

    /** Summaries most similar to the question. Summaries are many, unkeyed and varied - similarity is the only sensible access path. */
    List<ScoredSummary> searchSummaries(String userId, String query, int topK);

    List<ScoredSummary> findAllSummaries(String userId);

    void deleteAll(String userId);

    record ScoredSummary(TopicSummary summary, Double score, Instant updatedAt) {
    }

    /**
     * A stored fact with the moment it was written. Because an upsert rewrites the row, this is when
     * the value was last asserted - which is the useful reading: it tells you how fresh the fact is.
     */
    record StoredFact(ProfileFact fact, Instant updatedAt) {
    }
}
