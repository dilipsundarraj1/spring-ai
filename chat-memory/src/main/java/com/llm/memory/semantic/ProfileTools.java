package com.llm.memory.semantic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * The write side of semantic memory: the model calls this tool when the person states a lasting fact.
 * <p>
 * The user id comes from the {@link ToolContext}, which the controller fills and the model never sees - so the model
 * can only change the profile of the person it is talking to.
 */
@Component
public class ProfileTools {

    /** Key of the user id in the tool context. */
    public static final String USER_ID = "userId";

    private static final Logger log = LoggerFactory.getLogger(ProfileTools.class);

    private final UserProfileStore profileStore;

    public ProfileTools(UserProfileStore profileStore) {
        this.profileStore = profileStore;
    }

    @Tool(description = """
            Save a lasting fact about the person to their profile: their name, the vehicle they drive, their home
            branch, or how they want to be contacted. The new value replaces the old one.
            Do not use it for one-off details such as a single appointment.""")
    public String updateProfile(
            @ToolParam(description = "Which fact to save") ProfileField field,
            @ToolParam(description = "The new value, for example 'Honda Civic' or 'Text only, never call'") String value,
            ToolContext toolContext) {
        var userId = (String) toolContext.getContext().get(USER_ID);
        log.info("updateProfile - userId : {} , {} = {}", userId, field, value);
        profileStore.update(userId, field, value);
        return "Saved: " + field + " = " + value;
    }
}
