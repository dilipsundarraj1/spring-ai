package com.llm.memory.longterm;

/**
 * A short keyed assertion about the person, derived from Turns rather than quoted from them.
 * One per key per user - a later fact with the same key replaces it.
 */
public record ProfileFact(FactKey key, String statement) {
}
