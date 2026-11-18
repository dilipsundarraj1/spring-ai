# Derived long-term memory with retained conversation history

The `chat-memory` module demonstrates recall by recency (v1–v3) and recall by similarity over raw Turns (v4–v5).
Both fail at relevance: measured against this module's own data, every stored message scores 0.79–0.92 against a
question on an unrelated subject, because raw conversational Turns are poor retrieval units — a question and its
answer sit next to each other in embedding space. v6 therefore stores *derived* units instead: keyed Profile Facts
(loaded wholesale, replaced on correction) and subject-segmented Topic Summaries (retrieved by similarity), both
consolidated out of Chat Memory as it overflows, while the raw Turns are archived in an append-only Chat History so
Long-Term Memory can be re-derived whenever the extraction rules change.

## Considered Options

- **Profile Facts in a relational table** with `ON CONFLICT` upsert. More correct — a keyed lookup needs no embedding —
  but it adds a second storage idiom and schema management the course has not covered. We use one pgvector table
  discriminated by memory type, which also lets facts switch to semantic retrieval later without a migration.
- **Similarity search over Profile Facts.** Rejected: with a handful of keyed facts, top-K can only omit one that
  mattered, and short statements land in the same compressed score band measured above. A fact's key is a better
  index than its embedding.
- **Free-form fact keys.** Rejected: key drift (`VEHICLE` one turn, `CAR` the next) silently breaks replacement,
  which is the exact failure this design exists to prevent. The key set is a fixed four-value enum.
- **One Topic Summary per evicted batch.** Rejected: eviction batches are time slices, so one summary could span
  unrelated subjects and reproduce the bad-unit problem one level up. The extractor segments each batch by subject
  and may return none.
- **Fallback search over Chat History when Long-Term Memory misses.** Rejected for now: raw Turns score high against
  anything, so the fallback would fire on questions it cannot help. A keyword (`ILIKE`) fallback is the noted next
  step, because it dodges score compression entirely.
- **Absolute similarity thresholds as the primary filter.** Rejected: measured unrelated content at 0.79–0.92, so a
  naive floor filters nothing while a floor high enough to exclude noise also drops answers.

## Consequences

- Extraction quality is cheap to change. Improve the prompt or swap the model, then rebuild Long-Term Memory from
  Chat History — so the extraction model is the first knob to turn when facts look wrong.
- The key enum can be expanded retroactively (add a key, rebuild) but not renamed safely: a rename orphans stored
  rows whose key the enum no longer recognises.
