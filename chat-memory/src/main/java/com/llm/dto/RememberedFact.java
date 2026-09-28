package com.llm.dto;

/** A Profile Fact as returned by the API. */
import java.time.Instant;

public record RememberedFact(String key, String statement, Instant updatedAt) {
}
