package com.llm.dto;

/**
 * The answer plus both halves of the memory step: what was recalled to answer, and what
 * Consolidation learned on the way out. Makes the mechanism visible in one request.
 */
public record LongTermAnswer(String answer,
                             int windowSize,
                             int turnsInWindow,
                             MemorySnapshot recalled,
                             MemorySnapshot learned) {
}
