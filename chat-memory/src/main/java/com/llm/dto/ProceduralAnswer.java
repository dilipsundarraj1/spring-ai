package com.llm.dto;

import java.util.List;

/**
 * The model's answer, the profile after the call, the recalled episodes, and the procedures the model loaded while
 * answering - so the response shows which rules it followed.
 */
public record ProceduralAnswer(String answer,
                               UserProfile profile,
                               List<Episode> recalledEpisodes,
                               List<String> proceduresLoaded) {
}
