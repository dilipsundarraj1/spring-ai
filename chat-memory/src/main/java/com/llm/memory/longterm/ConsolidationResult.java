package com.llm.memory.longterm;

import java.util.List;

/**
 * What one Consolidation pass found. Both lists may be empty - six Turns of small talk correctly
 * consolidate to nothing.
 */
public record ConsolidationResult(List<ProfileFact> facts, List<TopicSummary> summaries) {

    public static ConsolidationResult empty() {
        return new ConsolidationResult(List.of(), List.of());
    }

    public ConsolidationResult {
        facts = facts == null ? List.of() : List.copyOf(facts);
        summaries = summaries == null ? List.of() : List.copyOf(summaries);
    }

    public boolean isEmpty() {
        return facts.isEmpty() && summaries.isEmpty();
    }
}
