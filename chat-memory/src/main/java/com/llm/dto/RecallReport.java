package com.llm.dto;

import java.util.List;

/**
 * What the retrieval step actually did: the knobs that were applied, how many rows the vector
 * store returned before filtering, and what survived.
 */
public record RecallReport(double similarityThreshold,
                           boolean dedupe,
                           int topK,
                           int fetchedFromStore,
                           int recalledCount,
                           List<RecalledMemory> recalled) {
}
