package com.llm.dto;

import java.util.List;

/**
 * The model's answer, the profile as it is <em>after</em> the call - so a fact saved during the call shows up - and
 * the episodes that were recalled.
 */
public record SemanticAnswer(String answer, UserProfile profile, List<Episode> recalledEpisodes) {
}
