package com.llm.memory.longterm;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * A ChatMemory that consolidates instead of merely forgetting.
 * <p>
 * MessageWindowChatMemory cannot be reused here: it is final, has a private constructor, and trims
 * silently inside add(...) with no eviction hook. So this owns the trimming, which is the whole
 * point - Turns about to leave the window are handed to Consolidation first, and only then dropped.
 * They survive in Chat History either way.
 * <p>
 * The conversation id carries both scopes as {@code userId::conversationId}: ChatMemory's API knows
 * only about a conversation, but derived memory belongs to the person, so Consolidation needs the
 * user. Use {@link #scopeOf} to build the key.
 * <p>
 * Deliberately NOT a Spring bean. ChatMemoryAutoConfiguration declares its ChatMemory with
 * {@code @ConditionalOnMissingBean}, so publishing this as a component would suppress the
 * auto-configured one and silently rewire v2 and v3 to this window. The controller constructs it,
 * the same way InMemoryChatController builds its own memory and for the same reason.
 */
public class ConsolidatingChatMemory implements ChatMemory {

    /**
     * Production would sit at 20+. Only the frequency of Consolidation changes with this number,
     * never the architecture - six keeps the whole lifecycle visible in a handful of requests.
     */
    public static final int WINDOW_SIZE = 6;

    private static final String SCOPE_SEPARATOR = "::";

    private final ChatMemoryRepository repository;
    private final ConsolidationService consolidationService;

    public ConsolidatingChatMemory(ConsolidationService consolidationService) {
        this(consolidationService, new InMemoryChatMemoryRepository());
    }

    ConsolidatingChatMemory(ConsolidationService consolidationService, ChatMemoryRepository repository) {
        this.consolidationService = consolidationService;
        this.repository = repository;
    }

    public static String scopeOf(String userId, String conversationId) {
        Assert.hasText(userId, "userId cannot be null or empty");
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        return userId + SCOPE_SEPARATOR + conversationId;
    }

    static String userIdOf(String scope) {
        int separator = scope.indexOf(SCOPE_SEPARATOR);
        Assert.isTrue(separator > 0, "scope must be userId" + SCOPE_SEPARATOR + "conversationId, was: " + scope);
        return scope.substring(0, separator);
    }

    @Override
    public void add(String scope, List<Message> messages) {
        addReporting(scope, messages);
    }

    /**
     * Same as {@link #add} but returns what Consolidation produced, so a caller can show it. The
     * ChatMemory contract returns void, which would otherwise hide the most interesting thing that
     * happens here.
     * <p>
     * Worked example, adding the fourth turn of a conversation with {@code WINDOW_SIZE = 6}:
     *
     * <pre>
     * before    [u1 a1 u2 a2 u3 a3]         6 messages - exactly full
     * append    [u1 a1 u2 a2 u3 a3 u4 a4]   8 messages - two over the limit
     * overflow  8 - 6 = 2, and window[2] is u2 (a USER), so the cut stays at 2
     * outgoing  [u1 a1]                     handed to Consolidation, which extracts
     *                                       NAME and VEHICLE from what u1 said
     * after     [u2 a2 u3 a3 u4 a4]         6 again - and the outgoing pair is not lost:
     *                                       it was archived in Chat History before this ran
     * returns   the facts and summaries just extracted
     * </pre>
     * <p>
     * The three turns before that one bring the window to 2, then 4, then 6 messages. None exceeds
     * the limit, so each returns an empty result without calling the LLM at all. That is why
     * Consolidation - and the second LLM call it costs - lands on every fourth turn rather than on
     * every turn, and why a fact stated in turn 2 does not reach Long-Term Memory until turn 5.
     */
    public ConsolidationResult addReporting(String scope, List<Message> messages) {
        Assert.hasText(scope, "scope cannot be null or empty");
        Assert.notNull(messages, "messages cannot be null");

        ConsolidationResult consolidated = ConsolidationResult.empty();
        List<Message> window = new ArrayList<>(repository.findByConversationId(scope));
        window.addAll(messages);

        if (window.size() > WINDOW_SIZE) {
            int overflow = window.size() - WINDOW_SIZE;
            int cut = completeTurnBoundary(window, overflow);
            if (cut > 0) {
                List<Message> outgoing = List.copyOf(window.subList(0, cut));
                consolidated = consolidationService.consolidate(userIdOf(scope), outgoing);
                window = new ArrayList<>(window.subList(cut, window.size()));
            }
        }
        repository.saveAll(scope, window);
        return consolidated;
    }

    @Override
    public List<Message> get(String scope) {
        Assert.hasText(scope, "scope cannot be null or empty");
        return repository.findByConversationId(scope);
    }

    /**
     * Clears every conversation window belonging to one person. Chat Memory is scoped per
     * conversation, so forgetting a person means clearing all of their windows - otherwise a repeated
     * demo starts with a half-full window and Consolidation fires a turn early.
     */
    public void clearUser(String userId) {
        String prefix = userId + SCOPE_SEPARATOR;
        repository.findConversationIds().stream()
                .filter(scope -> scope.startsWith(prefix))
                .forEach(repository::deleteByConversationId);
    }

    @Override
    public void clear(String scope) {
        Assert.hasText(scope, "scope cannot be null or empty");
        repository.deleteByConversationId(scope);
    }

    /**
     * Extends the cut forward to the next USER message so a Turn is never split - an assistant reply
     * is never consolidated without the user message that caused it. Same rule
     * MessageWindowChatMemory applies when it evicts, which is why the window can end up smaller
     * than WINDOW_SIZE.
     * <p>
     * With strict user/assistant alternation the overflow is always even and already lands on a USER
     * message, so this is a no-op. It earns its keep when a turn produces more than two messages:
     *
     * <pre>
     * window    [u1 a1 a1b u2 a2 u3 a3]   the first turn answered with two messages
     * overflow  1  ->  window[1] is a1, not a USER, so advance
     *                  window[2] is a1b, not a USER, so advance
     *                  window[3] is u2, a USER, so the cut is 3
     * outgoing  [u1 a1 a1b]               the whole first turn, rather than just u1
     * </pre>
     *
     * Cutting at 1 would have consolidated the question without either half of its answer, and left
     * two orphaned assistant messages at the head of the window.
     */
    private static int completeTurnBoundary(List<Message> window, int overflow) {
        int cut = overflow;
        while (cut < window.size() && window.get(cut).getMessageType() != MessageType.USER) {
            cut++;
        }
        return Math.min(cut, window.size());
    }
}
