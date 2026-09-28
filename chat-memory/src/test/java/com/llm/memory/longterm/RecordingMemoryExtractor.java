package com.llm.memory.longterm;

import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Stands in for the LLM extraction pass: records every batch it was handed, and returns whatever the
 * test tells it to. This is the seam that keeps the window and upsert logic testable.
 */
public class RecordingMemoryExtractor implements MemoryExtractor {

    private final List<List<Message>> batches = new ArrayList<>();
    private Function<List<Message>, ConsolidationResult> response = turns -> ConsolidationResult.empty();

    @Override
    public ConsolidationResult extract(List<Message> turns) {
        batches.add(List.copyOf(turns));
        return response.apply(turns);
    }

    public void respondWith(ConsolidationResult result) {
        this.response = turns -> result;
    }

    public void respondWith(Function<List<Message>, ConsolidationResult> responder) {
        this.response = responder;
    }

    public List<List<Message>> batches() {
        return List.copyOf(batches);
    }

    public int callCount() {
        return batches.size();
    }

    public List<String> lastBatchRendered() {
        return batches.getLast().stream().map(m -> m.getMessageType() + ": " + m.getText()).toList();
    }
}
