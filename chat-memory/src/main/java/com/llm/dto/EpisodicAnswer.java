package com.llm.dto;

import java.util.List;

/**
 * The model's answer plus the episodes that were handed to it, so the recall step is visible in the
 * response instead of only in the logs.
 */
public record EpisodicAnswer(String answer, List<Episode> recalledEpisodes) {
}
