package com.igot.cb.controller;

import com.igot.cb.model.ApiResponse;
import com.igot.cb.service.ContentDictionaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Content Dictionary operations.
 */
@RestController
@RequestMapping("/content/dictionary")
@Slf4j
public class ContentDictionaryController {

    private final ContentDictionaryService contentDictionaryService;

    public ContentDictionaryController(ContentDictionaryService contentDictionaryService) {
        this.contentDictionaryService = contentDictionaryService;
    }

    /**
     * REST endpoint to fetch the content dictionary from Redis.
     *
     * @return ResponseEntity containing the ApiResponse with the content dictionary payload.
     */
    @GetMapping("/v1/read")
    public ResponseEntity<ApiResponse> getContentDictionary() {
        log.info("ContentDictionaryController::getContentDictionary - Request received");
        ApiResponse response = contentDictionaryService.getContentDictionary();
        return new ResponseEntity<>(response, response.getResponseCode());
    }
}
