package com.llm.dto;

/** A Topic Summary as returned by the API, with the similarity score when it was retrieved by search. */
import java.time.Instant;

public record RememberedSummary(String subject, String note, Double score, Instant updatedAt) {
}
