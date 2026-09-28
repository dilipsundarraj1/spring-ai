package com.llm.memory.longterm;

/**
 * A self-contained note covering several Turns on one subject. Unlike a ProfileFact these
 * accumulate: nothing replaces them.
 */
public record TopicSummary(String subject, String note) {
}
