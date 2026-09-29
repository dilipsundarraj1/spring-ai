package com.llm.dto;

import org.springframework.ai.document.Document;

/**
 * A message recalled from a vector store, with both ways of expressing how close it was to the query.
 * <p>
 * Only one number is actually stored. {@code PgVectorStore} runs the cosine distance operator
 * ({@code embedding <=> query}) and sets {@code score = 1.0 - distance}, so the two are the same
 * measurement inverted:
 * <ul>
 * <li><b>distance</b> — 0.0 is identical, larger is further apart. This is what the index sorts on.</li>
 * <li><b>score</b> — 1.0 is identical, smaller is further apart. This is what {@code similarityThreshold}
 * compares against.</li>
 * </ul>
 * Both are shown because the SQL you write by hand deals in distance while the Spring AI API deals in
 * score, and mixing them up inverts your filter.
 */
public record ScoredMessage(String type, String text, Double score, Double distance) {

    public static ScoredMessage from(Document document, String messageTypeMetadataKey) {
        Double score = document.getScore();
        return new ScoredMessage(
                String.valueOf(document.getMetadata().getOrDefault(messageTypeMetadataKey, "UNKNOWN")),
                document.getText(),
                score,
                score == null ? null : 1.0 - score);
    }
}
