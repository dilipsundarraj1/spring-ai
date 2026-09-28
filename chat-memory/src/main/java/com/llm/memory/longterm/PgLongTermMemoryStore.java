package com.llm.memory.longterm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Derived memory in its own pgvector table.
 * <p>
 * Note what this class is NOT: a {@code VectorStore} bean. PgVectorStoreAutoConfiguration declares
 * its store with {@code @ConditionalOnMissingBean}, so publishing a second VectorStore bean would
 * suppress the auto-configured one and break v4/v5. Instead the store is built and owned here, and
 * {@code afterPropertiesSet()} is called by hand because Spring is not managing its lifecycle.
 */
@Component
public class PgLongTermMemoryStore implements LongTermMemoryStore {

    static final String TABLE_NAME = "chat_memory_long_term";

    private static final String USER_ID = "userId";
    private static final String MEMORY_TYPE = "memoryType";
    private static final String FACT_KEY = "factKey";
    private static final String SUBJECT = "subject";

    /** ISO-8601 write time. The table is PgVectorStore's, so extra columns are impossible - but its
     * metadata column is JSON, so this rides along and comes back on every read. */
    private static final String UPDATED_AT = "updatedAt";

    private static final String PROFILE_FACT = "PROFILE_FACT";
    private static final String TOPIC_SUMMARY = "TOPIC_SUMMARY";

    private final PgVectorStore vectorStore;

    public PgLongTermMemoryStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel,
            @Value("${spring.ai.vectorstore.pgvector.initialize-schema:false}") boolean initializeSchema) {
        // same property the auto-configured pgvector store reads, so one switch covers both. Schema
        // init runs "CREATE EXTENSION vector", which only Postgres understands - the HSQLDB tests
        // set it false.
        this.vectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(TABLE_NAME)
                .initializeSchema(initializeSchema)
                .build();
        this.vectorStore.afterPropertiesSet();
    }

    @Override
    public void upsertFact(String userId, ProfileFact fact) {
        var builder = new FilterExpressionBuilder();
        vectorStore.delete(builder
                .and(builder.and(builder.eq(USER_ID, userId), builder.eq(MEMORY_TYPE, PROFILE_FACT)),
                        builder.eq(FACT_KEY, fact.key().name()))
                .build());

        vectorStore.add(List.of(Document.builder()
                .text(fact.statement())
                .metadata(Map.of(USER_ID, userId, MEMORY_TYPE, PROFILE_FACT, FACT_KEY, fact.key().name(),
                        UPDATED_AT, Instant.now().toString()))
                .build()));
    }

    @Override
    public void addSummary(String userId, TopicSummary summary) {
        vectorStore.add(List.of(Document.builder()
                // the subject is embedded along with the note: a question about "caching" should
                // match a note whose subject is caching even if the wording differs
                .text(summary.subject() + ": " + summary.note())
                .metadata(Map.of(USER_ID, userId, MEMORY_TYPE, TOPIC_SUMMARY, SUBJECT, summary.subject(),
                        UPDATED_AT, Instant.now().toString()))
                .build()));
    }

    @Override
    public List<StoredFact> findFacts(String userId) {
        // read wholesale - a handful of rows, each with an exact key, so similarity would only risk
        // omitting one that mattered. The query text is irrelevant; the filter does the work.
        return search(userId, PROFILE_FACT, "profile", 100).stream()
                .map(document -> new StoredFact(
                        new ProfileFact(FactKey.valueOf(String.valueOf(document.getMetadata().get(FACT_KEY))),
                                document.getText()),
                        updatedAtOf(document)))
                .toList();
    }

    @Override
    public List<ScoredSummary> searchSummaries(String userId, String query, int topK) {
        return toScoredSummaries(search(userId, TOPIC_SUMMARY, query, topK));
    }

    @Override
    public List<ScoredSummary> findAllSummaries(String userId) {
        return toScoredSummaries(search(userId, TOPIC_SUMMARY, "summary", 100));
    }

    @Override
    public void deleteAll(String userId) {
        vectorStore.delete(new FilterExpressionBuilder().eq(USER_ID, userId).build());
    }

    private List<Document> search(String userId, String memoryType, String query, int topK) {
        var builder = new FilterExpressionBuilder();
        Filter.Expression filter = builder.and(builder.eq(USER_ID, userId), builder.eq(MEMORY_TYPE, memoryType))
                .build();

        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(topK)
                .filterExpression(filter)
                .build());

        return documents == null ? List.of() : documents;
    }

    /** Rows written before this column existed simply have no timestamp - hence the null. */
    private static Instant updatedAtOf(Document document) {
        Object value = document.getMetadata().get(UPDATED_AT);
        try {
            return value == null ? null : Instant.parse(value.toString());
        }
        catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static List<ScoredSummary> toScoredSummaries(List<Document> documents) {
        return documents.stream()
                .map(document -> new ScoredSummary(
                        new TopicSummary(String.valueOf(document.getMetadata().get(SUBJECT)), document.getText()),
                        document.getScore(), updatedAtOf(document)))
                .toList();
    }
}
