package com.llm.dto;

import java.util.List;

/** A view of Long-Term Memory: what was recalled, what was learned, or everything stored. */
public record MemorySnapshot(List<RememberedFact> facts, List<RememberedSummary> summaries) {

    public static MemorySnapshot empty() {
        return new MemorySnapshot(List.of(), List.of());
    }
}
