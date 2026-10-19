package com.llm.dto;

/**
 * One procedure from {@code resources/procedures}: its name and the one-line description the model sees in the index.
 * The steps are not here - the model loads them with the loadProcedure tool when a request matches.
 */
public record Procedure(String name, String description) {
}
