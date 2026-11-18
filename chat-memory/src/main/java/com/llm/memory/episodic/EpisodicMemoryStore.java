package com.llm.memory.episodic;

import com.llm.dto.Episode;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Episodic memory: one row per ended conversation, in its own pgvector table.
 * <p>
 * The {@code PgVectorStore} is built here rather than published as a bean.
 * PgVectorStoreAutoConfiguration declares its store with {@code @ConditionalOnMissingBean}, so a
 * second VectorStore bean would switch off the auto-configured one that v4 and v5 use.
 */
@Component
public class EpisodicMemoryStore {

    static final String TABLE_NAME = "episodic_memory";

    private static final String USER_ID = "userId";
    private static final String CONVERSATION_ID = "conversationId";

    /** Epoch seconds, a JSON number - so the recency filter can compare it with {@code >=}. */
    private static final String ENDED_AT = "endedAt";

    /** The same moment as a date, for people and for the prompt. */
    private static final String ENDED_ON = "endedOn";

    private final PgVectorStore vectorStore;

    public EpisodicMemoryStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel,
            @Value("${spring.ai.vectorstore.pgvector.initialize-schema:false}") boolean initializeSchema) {
        // Same switch as the auto-configured store. Schema init runs "CREATE EXTENSION vector", which
        // only Postgres understands - the HSQLDB tests turn it off.
        this.vectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(TABLE_NAME)
                .initializeSchema(initializeSchema)
                .build();
        // Spring is not managing this store, so its lifecycle callback has to be called by hand
        this.vectorStore.afterPropertiesSet();
    }

    public Episode save(String userId, String conversationId, String summary, Instant endedAt) {
        String endedOn = endedAt.atZone(ZoneOffset.UTC).toLocalDate().toString();
        vectorStore.add(List.of(Document.builder()
                .text(summary)                               // only the summary is embedded
                .metadata(Map.of(
                        USER_ID, userId,
                        CONVERSATION_ID, conversationId,
                        ENDED_AT, endedAt.getEpochSecond(),
                        ENDED_ON, endedOn))
                .build()));
        return new Episode(conversationId, endedOn, summary, null);
    }

    /**
     * Step 1 filters by user and recency, step 2 ranks what is left by similarity to the question.
     */
    public List<Episode> search(String userId, String question, int recentDays, int topK) {
        long since = Instant.now().minus(Duration.ofDays(recentDays)).getEpochSecond();

        var searchRequest = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .filterExpression(userAndRecency(userId, since))
                .build();

        return vectorStore.similaritySearch(searchRequest)
                .stream()
                .map(EpisodicMemoryStore::toEpisode)
                .toList();
    }

    public void deleteAll(String userId) {
        vectorStore.delete(new FilterExpressionBuilder().eq(USER_ID, userId).build());
    }

    private static Filter.Expression userAndRecency(String userId, long since) {
        var builder = new FilterExpressionBuilder();
        return builder.and(builder.eq(USER_ID, userId), builder.gte(ENDED_AT, since)).build();
    }

    private static Episode toEpisode(Document document) {
        var metadata = document.getMetadata();
        return new Episode(
                String.valueOf(metadata.get(CONVERSATION_ID)),
                String.valueOf(metadata.get(ENDED_ON)),
                document.getText(),
                document.getScore());
    }
}
