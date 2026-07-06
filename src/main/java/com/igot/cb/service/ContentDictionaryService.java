package com.igot.cb.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.cache.RedisCacheMgr;
import com.igot.cb.model.ApiResponse;
import com.igot.cb.util.Constants;
import com.igot.cb.util.ProjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service class for handling operations related to Content Dictionary.
 */
@Service
@Slf4j
public class ContentDictionaryService {

    private final RedisCacheMgr redisCacheMgr;
    private final ObjectMapper objectMapper;

    public ContentDictionaryService(RedisCacheMgr redisCacheMgr, ObjectMapper objectMapper) {
        this.redisCacheMgr = redisCacheMgr;
        this.objectMapper = objectMapper;
    }

    /**
     * Reads the content dictionary from Redis key "content_dictionary"
     * and constructs the API Response.
     *
     * @return ApiResponse containing the content dictionary as result map or error status.
     */
    public ApiResponse getContentDictionary() {
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_CONTENT_DICTIONARY_READ);
        try {
            String value = redisCacheMgr.getFromCache(Constants.REDIS_CONTENT_DICTIONARY_KEY);
            if (StringUtils.isBlank(value)) {
                log.warn("ContentDictionaryService::getContentDictionary - Content dictionary key '{}' not found or empty in Redis", Constants.REDIS_CONTENT_DICTIONARY_KEY);
                ProjectUtil.setFailedResponse(response, "Content dictionary not found in cache", HttpStatus.NOT_FOUND);
                return response;
            }

            Map<String, Object> dictionary = objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
            
            response.setResult(dictionary);
            response.setResponseCode(HttpStatus.OK);
            log.info("ContentDictionaryService::getContentDictionary - Successfully retrieved content dictionary");
        } catch (Exception e) {
            log.error("ContentDictionaryService::getContentDictionary - Error retrieving content dictionary from Redis", e);
            ProjectUtil.setFailedResponse(response, "Failed to read content dictionary: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }
}
