package com.llm.controller;

import com.llm.service.IngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@RestController
public class IngestionController {

    private static final Logger log = LoggerFactory.getLogger(IngestionController.class);

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /**
     * Endpoint to handle file uploads for ingestion.
     *
     * @param file the file to be ingested
     * @return a response indicating the success or failure of the operation
     */
    @PostMapping("/api/v1/files/ingest")
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam("ingestType") String ingestType) {
        try {
            // Process the file content
            byte[] fileContent = file.getBytes();

            // Ingest the content into your vector database
            ingestionService.ingest(fileContent, file.getOriginalFilename(),ingestType);
            return ResponseEntity.ok("File uploaded and processed successfully.");
        } catch (Exception e){
            return ResponseEntity.status(500).body("Failed to process the file.");
        }
    }

}
