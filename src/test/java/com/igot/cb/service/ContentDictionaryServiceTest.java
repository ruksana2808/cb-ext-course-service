package com.igot.cb.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.cache.RedisCacheMgr;
import com.igot.cb.model.ApiResponse;
import com.igot.cb.util.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Unit tests for ContentDictionaryService.
 */
@ExtendWith(MockitoExtension.class)
class ContentDictionaryServiceTest {

    @Mock
    private RedisCacheMgr redisCacheMgr;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ContentDictionaryService contentDictionaryService;

    @Test
    void testGetContentDictionary_Success() throws Exception {
        Map<String, Object> testMap = Map.of("key1", "value1", "key2", 123);
        String jsonValue = objectMapper.writeValueAsString(testMap);

        when(redisCacheMgr.getFromCache(Constants.REDIS_CONTENT_DICTIONARY_KEY)).thenReturn(jsonValue);

        ApiResponse response = contentDictionaryService.getContentDictionary();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
        assertEquals("value1", response.getResult().get("key1"));
        assertEquals(123, response.getResult().get("key2"));
    }

    @Test
    void testGetContentDictionary_CacheEmpty() {
        when(redisCacheMgr.getFromCache(Constants.REDIS_CONTENT_DICTIONARY_KEY)).thenReturn(null);

        ApiResponse response = contentDictionaryService.getContentDictionary();

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals("Content dictionary not found in cache", response.getParams().getErrMsg());
    }

    @Test
    void testGetContentDictionary_InvalidJson() {
        when(redisCacheMgr.getFromCache(Constants.REDIS_CONTENT_DICTIONARY_KEY)).thenReturn("invalid-json");

        ApiResponse response = contentDictionaryService.getContentDictionary();

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertNotNull(response.getParams().getErrMsg());
    }
}
