package com.igot.cb.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.igot.cb.model.ApiResponse;
import com.igot.cb.service.ContentDictionaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for ContentDictionaryController.
 */
@ExtendWith(MockitoExtension.class)
class ContentDictionaryControllerTest {

    @Mock
    private ContentDictionaryService contentDictionaryService;

    @InjectMocks
    private ContentDictionaryController contentDictionaryController;

    @Test
    void testGetContentDictionary() {
        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);

        when(contentDictionaryService.getContentDictionary()).thenReturn(mockResponse);

        ResponseEntity<ApiResponse> response = contentDictionaryController.getContentDictionary();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(contentDictionaryService).getContentDictionary();
    }
}
