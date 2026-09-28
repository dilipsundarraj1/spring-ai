package com.llm.memory.longterm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Consolidation: extract derived memory from Turns leaving Chat Memory, then write it. Facts are
 * upserted by key so a correction replaces the stale value; summaries are appended.
 */
@Service
public class ConsolidationService {

    private static final Logger log = LoggerFactory.getLogger(ConsolidationService.class);

    private final MemoryExtractor extractor;
    private final LongTermMemoryStore store;

    public ConsolidationService(MemoryExtractor extractor, LongTermMemoryStore store) {
        this.extractor = extractor;
        this.store = store;
    }

    public ConsolidationResult consolidate(String userId, List<Message> turns) {
        log.info("Consolidating {} outgoing turn(s) for user {}", turns.size(), userId);
        turns.forEach(turn -> log.info("  outgoing turn | {} : {}", turn.getMessageType(), turn.getText()));

        ConsolidationResult result = extractor.extract(turns);
        if (result.isEmpty()) {
            log.info("Nothing worth remembering in {} turn(s) for user {}", turns.size(), userId);
            return result;
        }

        // facts replace by key, summaries accumulate - the only difference between the two writes
        result.facts().forEach(fact -> {
            log.info("  fact upserted  | {} -> {}", fact.key(), fact.statement());
            store.upsertFact(userId, fact);
        });
        result.summaries().forEach(summary -> {
            log.info("  summary added  | {} -> {}", summary.subject(), summary.note());
            store.addSummary(userId, summary);
        });
        return result;
    }

    /**
     * Re-derives everything for a user from Chat History. This is what retaining raw Turns buys:
     * add a FactKey or improve the extraction prompt, then rebuild, and facts appear that were
     * mentioned conversations ago.
     */
    public ConsolidationResult rebuild(String userId, List<Message> allTurns, int batchSize) {
        // Extract everything FIRST, then swap. Deleting up front would mean a flaky extraction leaves
        // the person with less memory than they started with - and extraction is flaky enough that
        // this happened in practice.
        var facts = new ArrayList<ProfileFact>();
        var summaries = new ArrayList<TopicSummary>();

        for (int start = 0; start < allTurns.size(); start += batchSize) {
            List<Message> batch = allTurns.subList(start, Math.min(start + batchSize, allTurns.size()));
            ConsolidationResult batchResult = extractor.extract(batch);
            facts.addAll(batchResult.facts());
            summaries.addAll(batchResult.summaries());
        }

        if (facts.isEmpty() && summaries.isEmpty()) {
            log.warn("Rebuild for user {} extracted nothing from {} turn(s); keeping existing memory", userId,
                    allTurns.size());
            return ConsolidationResult.empty();
        }

        store.deleteAll(userId);
        // later batches win for a given key, which is what we want: corrections come after the
        // statement they correct
        facts.forEach(fact -> store.upsertFact(userId, fact));
        summaries.forEach(summary -> store.addSummary(userId, summary));

        return new ConsolidationResult(facts, summaries);
    }
}
