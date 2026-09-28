package com.llm.dto;

/** Result of re-deriving Long-Term Memory from Chat History. */
public record RebuildReport(int turnsProcessed, int batchSize, MemorySnapshot rebuilt) {
}
