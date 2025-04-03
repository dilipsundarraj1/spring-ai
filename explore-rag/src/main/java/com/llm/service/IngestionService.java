package com.llm.service;

import com.llm.controller.IngestionController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

//@Component
public class IngestionService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final VectorStore vectorStore;
    @Value("classpath:/docs/Flexora_FAQ.pdf")
    private Resource faqPdf;

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) throws Exception {

        var pdfReader = new PagePdfDocumentReader(faqPdf).get();
        vectorStore.add(pdfReader);
        log.info("VectorStore: {}", vectorStore);
//        pdfReader.read().forEach(page -> {
//            log.info("Page: {}", page);
//
//        });

    }
}
