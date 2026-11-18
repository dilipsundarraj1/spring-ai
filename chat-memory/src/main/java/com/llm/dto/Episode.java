package com.llm.dto;

/**
 * One episode: a dated summary of a conversation that has ended. {@code score} is the similarity to
 * the question that recalled it, and is null when the episode was just written.
 */
public record Episode(String conversationId, String endedOn, String summary, Double score) {
}
