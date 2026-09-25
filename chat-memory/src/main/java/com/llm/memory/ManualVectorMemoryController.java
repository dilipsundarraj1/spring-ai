package com.llm.memory;

import com.llm.dto.MemoryAwareAnswer;
import com.llm.dto.RecallReport;
import com.llm.dto.RecalledMemory;
import com.llm.dto.UserInput;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Semantic chat memory without VectorStoreChatMemoryAdvisor. The retrieval is done here, so we
 * control the three things the advisor does not expose: a similarity threshold, a relative score
 * cut-off, and de-duplication. Every response reports what was recalled and at what score.
 */
@RestController
public class ManualVectorMemoryController {

    private static final Logger log = LoggerFactory.getLogger(ManualVectorMemoryController.class);

}
