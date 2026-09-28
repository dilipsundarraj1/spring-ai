#!/usr/bin/env bash
#
# The v6 acceptance demo, end to end: a fact stated in one conversation, recalled in a second,
# corrected there, and answered correctly in a third - with the stale value gone, not alongside.
# Nothing in v1-v5 can do this.
#
# Usage:  ./long-term-memory-demo.sh [userId]
#
# Requires the app running on 8084 (OPENAI_KEY set) and Postgres up (docker compose up pgvector -d).
# Costs roughly 20 OpenAI calls: 10 answers plus one Consolidation per window overflow.

set -euo pipefail

BASE="http://localhost:8084/springai/v6/memory/users"
USER_ID="${1:-dilip}"

# WINDOW_SIZE = 6 messages = 3 turn pairs. Consolidation fires on the turn that would overflow the
# window and processes the OLDEST pair. So a fact stated in turn 1 is consolidated on turn 4, and a
# fact stated in turn 2 is consolidated on turn 5. Those two numbers are why the filler turns below
# exist - they are not padding, they are what drives memory out of the window.

say() {   # say <conversationId> <prompt>
    local conversation="$1" prompt="$2"
    printf '\n\033[1m> %s\033[0m  (%s)\n' "$prompt" "$conversation"
    curl -s -X POST "$BASE/$USER_ID/chats/$conversation" \
        -H "Content-Type: application/json" \
        -d "{\"prompt\": $(printf '%s' "$prompt" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))')}" \
    | python3 -c '
import sys, json
raw = sys.stdin.read()
try:
    d = json.loads(raw)
except Exception:
    print("  ! unparseable response:", raw[:200]); raise SystemExit(1)
if "answer" not in d:
    print("  ! error:", raw[:200]); raise SystemExit(1)
print("  answer  :", (d["answer"] or "").replace("\n", " ")[:100])
print("  window  :", d["turnsInWindow"], "/", d["windowSize"])
r = d["recalled"]
if r["facts"]:
    print("  recalled:", ", ".join(x["key"] + "=" + x["statement"] for x in r["facts"]))
l = d["learned"]
if l["facts"] or l["summaries"]:
    print("  CONSOLIDATED:")
    for f in l["facts"]:
        print("     fact   ", f["key"], "->", f["statement"])
    for s in l["summaries"]:
        print("     summary", s["subject"])
'
}

memory() {
    printf '\n\033[1m=== stored long-term memory ===\033[0m\n'
    curl -s "$BASE/$USER_ID/memory" | python3 -c '
import sys, json
d = json.load(sys.stdin)
for f in d["facts"]:
    print("  FACT    ", f["key"], "->", f["statement"])
for s in d["summaries"]:
    print("  SUMMARY ", s["subject"])
if not d["facts"]:
    print("  (no facts yet)")
'
}

printf '\033[1m### Resetting user %s (derived memory + raw archive)\033[0m\n' "$USER_ID"
curl -s -o /dev/null -w '  DELETE -> %{http_code}\n' -X DELETE "$BASE/$USER_ID"

printf '\n\033[1m### Conversation A - state the facts\033[0m\n'
say conv-a "My name is Dilip and I drive a blue Tesla"
say conv-a "I work as a software architect"
say conv-a "I live in Dallas"
# turn 4: the window overflows for the first time, so the OLDEST pair - the name and the car -
# is consolidated into Long-Term Memory here
say conv-a "Tell me a joke about databases"
# turn 5: a smoke test. It also consolidates turn 2, so OCCUPATION lands here. Note this does NOT
# prove memory works - the Tesla statement is still in the window too. Conversation B is the real test.
say conv-a "What car do I drive?"

memory   # expect NAME, VEHICLE (blue Tesla), OCCUPATION

printf '\n\033[1m### Conversation B - a brand new conversationId: empty window, nothing shared but memory\033[0m\n'
say conv-b "What car do I drive?"                        # answered from Long-Term Memory alone
say conv-b "I sold the Tesla, I drive a Honda Civic now" # turn 2 -> consolidated on turn 5
say conv-b "What is the capital of France?"
say conv-b "Thanks, that helps"
# turn 5: the second pair leaves the window, which is the correction. VEHICLE is replaced here.
say conv-b "Anything else I should know about Dallas?"

memory   # expect VEHICLE = Honda Civic, and only ONE vehicle fact

printf '\n\033[1m### Conversation C - a third conversation sees the corrected fact\033[0m\n'
say conv-c "What car do I drive?"

memory

# An unrelated question, to see what memory contributes when it cannot help. Facts are read wholesale
# so all of them arrive regardless; the summary scores land in a band a hundredth of a point wide,
# which is the same score compression that made similarity over raw Turns useless.
printf '\n\033[1m### An unrelated question - what does memory contribute?\033[0m\n'
say conv-weather "Whats the weather?"

cat <<'NOTES'

------------------------------------------------------------------------------
What to look for

  * Conversation B's first answer is correct with an EMPTY window - it came from
    Long-Term Memory, not from replayed turns. v1-v5 cannot do this.
  * VEHICLE is replaced, not duplicated: one fact, not two. That is the keyed
    upsert working.
  * Consolidation appears on turns 4 and 5, never earlier - the window has to
    overflow first.

If the correction has not landed
  Extraction is a judgement call and can miss. Re-derive from the archive:

    curl -X POST http://localhost:8084/springai/v6/memory/users/USER/rebuild

  Rebuild replays every archived Turn through Consolidation, and later batches
  win for a given key - so the correction overrides the statement it corrects.
  This is the whole reason raw Turns are kept.

To run again
  Pass a fresh id (./long-term-memory-demo.sh dilip-2), or let the script's
  DELETE reset this one.
------------------------------------------------------------------------------
NOTES
