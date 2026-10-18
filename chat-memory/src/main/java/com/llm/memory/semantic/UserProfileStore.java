package com.llm.memory.semantic;

import com.llm.dto.UserProfile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

/**
 * Semantic memory: one row per person in a plain relational table. Facts are read with a lookup by user id - no
 * embeddings, no similarity search.
 */
@Component
public class UserProfileStore {

    static final String TABLE_NAME = "user_profile";

    private final JdbcClient jdbcClient;

    public UserProfileStore(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
        // created on startup to keep the demo self-contained; a real application would use a migration tool
        jdbcClient.sql("""
                CREATE TABLE IF NOT EXISTS user_profile (
                    user_id            VARCHAR(255) PRIMARY KEY,
                    name               VARCHAR(255),
                    vehicle            VARCHAR(255),
                    home_branch        VARCHAR(255),
                    contact_preference VARCHAR(255),
                    updated_at         TIMESTAMP
                )""").update();
    }

    public Optional<UserProfile> find(String userId) {
        return jdbcClient.sql("""
                        SELECT name, vehicle, home_branch, contact_preference
                        FROM user_profile WHERE user_id = :userId""")
                .param("userId", userId)
                .query((rs, rowNum) -> new UserProfile(
                        rs.getString("name"),
                        rs.getString("vehicle"),
                        rs.getString("home_branch"),
                        rs.getString("contact_preference")))
                .optional();
    }

    /**
     * Replaces one fact. The first fact for a person creates their row.
     */
    public void update(String userId, ProfileField field, String value) {
        // the column comes from the enum, never from the model, so it is safe to put into the SQL
        var now = Timestamp.from(Instant.now());
        int updated = jdbcClient.sql("UPDATE user_profile SET " + field.column() + " = :value, updated_at = :now"
                        + " WHERE user_id = :userId")
                .param("value", value)
                .param("now", now)
                .param("userId", userId)
                .update();
        if (updated == 0) {
            jdbcClient.sql("INSERT INTO user_profile (user_id, " + field.column() + ", updated_at)"
                            + " VALUES (:userId, :value, :now)")
                    .param("userId", userId)
                    .param("value", value)
                    .param("now", now)
                    .update();
        }
    }

    public void delete(String userId) {
        jdbcClient.sql("DELETE FROM user_profile WHERE user_id = :userId")
                .param("userId", userId)
                .update();
    }
}
