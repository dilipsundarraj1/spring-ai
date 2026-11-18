package com.llm.memory.semantic;

/**
 * The facts the profile can hold - a fixed list, so a new value always lands on the same field and replaces the old
 * one. The model picks a constant; it never names a column.
 */
public enum ProfileField {

    NAME("name"),
    VEHICLE("vehicle"),
    HOME_BRANCH("home_branch"),
    CONTACT_PREFERENCE("contact_preference");

    private final String column;

    ProfileField(String column) {
        this.column = column;
    }

    String column() {
        return column;
    }
}
