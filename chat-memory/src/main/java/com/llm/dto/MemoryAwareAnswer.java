package com.llm.dto;

/**
 * The model's answer plus the retrieval that produced its context, so the memory step is
 * visible instead of hidden inside an advisor.
 */
public record MemoryAwareAnswer(String answer, RecallReport memory) {
}
