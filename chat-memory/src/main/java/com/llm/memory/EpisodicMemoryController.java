package com.llm.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

/**
 * Episodic memory: when a conversation ends it is summarised into one dated episode, and later
 * conversations recall the episodes that match the question.
 * <p>
 * Conversation memory is the auto-configured JDBC one (as in v2), so it lives outside the
 * application and any instance can serve the next message.
 */
@RestController
public class EpisodicMemoryController {

    private static final Logger log = LoggerFactory.getLogger(EpisodicMemoryController.class);

}
