//package com.llm.rag;
//
//import com.llm.dto.AIResponse;
//import com.llm.dto.UserInput;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
//import org.springframework.ai.chat.prompt.PromptTemplate;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.model.ModelOptionsUtils;
//import org.springframework.ai.vectorstore.SearchRequest;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.io.Resource;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.HashMap;
//
//@RestController
//public class RAGController {
//    Logger log = LoggerFactory.getLogger(RAGController.class);
//
//    @Value("classpath:/prompt-templates/rag-template.st")
//    private Resource promptTemplateWithContext;
//
//    private final ChatClient chatClient;
//
//    private final VectorStore vectorStore;
//
//    @Autowired
//    @Qualifier("tikaSimpleVectorStore")
//    private VectorStore tikaSimpleVectorStore;
//
//    public RAGController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
//        this.chatClient = chatClientBuilder.build();
//        this.vectorStore = vectorStore;
//    }
//
//    @PostMapping("/rag")
//    public String rag(@RequestBody UserInput userInput) {
//        log.info("userInput : {} ", userInput);
//
//
//        var documents = vectorStore.similaritySearch(SearchRequest.query(userInput.prompt()).withTopK(2));
//        log.info("Retrieved documents : {} ", documents.size());
//
//        var contents = documents.stream().map(Document::getContent).toList();
//
//
//        var inputMap = new HashMap<String, Object>();
//        inputMap.put("documents", String.join("\n", contents));
//        inputMap.put("input", userInput.prompt());
//
//        log.info("inputMap : {} ", inputMap);
//
//        var promptTemplate = new PromptTemplate(promptTemplateWithContext);
//        log.info("promptTemplate : {} ", promptTemplateWithContext);
//
//        var prompt = promptTemplate.create(inputMap);
//
//        log.info("prompt : {} ", prompt);
//
//        var requestSpec = chatClient.prompt(prompt);
//
//        var responseSpec = requestSpec.call();
//        log.info("responseSpec : {} ", responseSpec.chatResponse());
//        return responseSpec.content();
//    }
//
//    @PostMapping("/v2/rag")
//    public AIResponse ragV2(@RequestBody UserInput userInput) {
//        log.info("userInput : {} ", userInput);
//
//        var documents = tikaSimpleVectorStore.similaritySearch(SearchRequest.query(userInput.prompt()).withTopK(2));
//        log.info("Retrieved documents : {} ", documents.size());
////        log.info("Retrieved Content is  : {} ", documents.getFirst().getContent());
//        var contents = documents.stream().map(Document::getContent).toList();
//
//        var response = chatClient
//                .prompt()
//                .advisors(new SimpleLoggerAdvisor(ModelOptionsUtils::toJsonString,
//                        ModelOptionsUtils::toJsonString))
//                .user(userInput.prompt())
//                .system(promptSystemSpec -> promptSystemSpec
//                        .text(promptTemplateWithContext)
//                        .param("documents", documents)
//                        .param("input", userInput.prompt()))
//                .call()
//                        .entity(AIResponse.class);
//
//        log.info("response  : {} ", response);
//        return response;
//    }
//
//}
