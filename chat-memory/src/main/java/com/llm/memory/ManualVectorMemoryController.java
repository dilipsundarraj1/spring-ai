package com.llm.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

/**
 * Semantic chat memory without VectorStoreChatMemoryAdvisor. The retrieval is done here, so we
 * control the three things the advisor does not expose: a similarity threshold, a relative score
 * cut-off, and de-duplication. Every response reports what was recalled and at what score.
 */
@RestController
public class ManualVectorMemoryController {

    private static final Logger log = LoggerFactory.getLogger(ManualVectorMemoryController.class);

}
