package com.llm.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chat memory stored in a vector store (pgvector). Instead of replaying the last N messages,
 * only the past messages that are semantically similar to the current question are retrieved
 * and appended to the system message.
 */
@RestController
public class VectorStoreChatMemoryController {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreChatMemoryController.class);


}
