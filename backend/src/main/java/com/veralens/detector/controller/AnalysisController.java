package com.veralens.detector.controller;

import com.veralens.detector.model.AnalysisResult;
import com.veralens.detector.service.ImageAnalysisService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * AnalysisController
 *
 * Exposes a single REST endpoint:
 *   POST /analyze
 *   Content-Type: multipart/form-data
 *   Field: "image" — the uploaded image file
 *
 * Returns:
 *   200 OK — { result, confidence, rules }
 *   400 Bad Request — if no file or invalid file
 *   500 Internal Server Error — unexpected processing failure
 */
@RestController
@RequestMapping("/analyze")
@CrossOrigin(origins = "*")   // Allow requests from the frontend on any port
public class AnalysisController {

    private final ImageAnalysisService analysisService;

    public AnalysisController(ImageAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> analyze(@RequestParam("image") MultipartFile file) {

        // Validate: file must be present
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("No image file provided.");
        }

        // Validate: content-type must be image/*
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest()
                .body("Invalid file type. Please upload a JPEG, PNG, or WebP image.");
        }

        // Validate: size limit 10MB
        if (file.getSize() > 10L * 1024 * 1024) {
            return ResponseEntity.badRequest()
                .body("File too large. Maximum allowed size is 10MB.");
        }

        try {
            AnalysisResult result = analysisService.analyze(file);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                .body("Failed to read image: " + e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Unexpected error during analysis: " + e.getMessage());
        }
    }
}
