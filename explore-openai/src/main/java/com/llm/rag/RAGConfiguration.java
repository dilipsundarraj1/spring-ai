package com.llm.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Configuration
public class RAGConfiguration {

    Logger log = LoggerFactory.getLogger(RAGConfiguration.class);

    @Value("vector_db.json")
    private String vectorDB;

    @Value("classpath:/files/fabletics-faq.txt")
    private Resource fablieticsFaq; //https://fabletics.thredup.com/pages/faq

    @Bean(name = "tikaSimpleVectorStore")

    VectorStore vectorStore(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel){
        return  SimpleVectorStore.builder(embeddingModel).build();
//        return  new SimpleVectorStore(embeddingModel);
    }

    @Bean
    ApplicationRunner go(@Qualifier("tikaSimpleVectorStore") VectorStore vectorStore){
        return args ->  {
            log.info("Read Docs using TikaDocumentReader");
            var documents= new TikaDocumentReader(fablieticsFaq).get();
            var fabFabDoc = documents.getFirst();
            var splitDocs = new TokenTextSplitter().split(fabFabDoc);
            log.info("splitDocs size: {} ", splitDocs.size());
            vectorStore.add(splitDocs);
        };
    }

    @Bean
    @Primary
    SimpleVectorStore simpleVectorStore(@Qualifier("openAiEmbeddingModel")EmbeddingModel embeddingModel) throws IOException {
        var simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        var vectorStoreFile = getVectorStoreFile();
        if (vectorStoreFile.exists()) {
            log.info("Vector Store File Exists, so loading the vector store directly from the file.");
            simpleVectorStore.load(vectorStoreFile);
        } else {
            log.info("Vector Store File Does Not Exist, loading documents");
            TextReader textReader = new TextReader(fablieticsFaq);
            textReader.getCustomMetadata().put("filename", "fabletics-faq.txt");
            List<Document> documents = textReader.get();
            //TextSplitter textSplitter = new TokenTextSplitter();
            TextSplitter textSplitter = new TokenTextSplitter(200, 350,5, 10000, true);
            List<Document> splitDocuments = textSplitter.apply(documents);
            log.info("splitDocuments : {}, splitDocuments size : {} ", splitDocuments, splitDocuments.size());
            simpleVectorStore.add(splitDocuments);
            log.info("Before saving the documents");
            simpleVectorStore.save(vectorStoreFile);
        }
        return simpleVectorStore;
    }

    private File getVectorStoreFile() {
        // Construct the base directory using File
        File directory = new File("explore-openai/src/main/resources/data");

        // Create the full path by appending the vector store name
        File vectorStoreFile = new File(directory, vectorDB);

        // Return the File object pointing to the vector store
        return vectorStoreFile.getAbsoluteFile();
    }


}
