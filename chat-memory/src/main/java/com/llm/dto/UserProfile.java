package com.llm.dto;

/**
 * Semantic memory: the standing facts about one person. Every field holds the <em>current</em> value, or null when
 * it is not known yet.
 */
public record UserProfile(String name, String vehicle, String homeBranch, String contactPreference) {

    public static final UserProfile EMPTY = new UserProfile(null, null, null, null);
}
