package com.llm.memory.longterm;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * Turns a batch of raw Turns into derived memory. This is the seam that keeps Consolidation
 * testable: the tests stub it, so none of the window or upsert logic needs an LLM.
 */
public interface MemoryExtractor {

    ConsolidationResult extract(List<Message> turns);
}
