package com.igot.cb.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.common.ServerProperties;
import com.igot.cb.util.Constants;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.igot.common.service.OutboundRequestHandlerServiceImpl;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class VideoOnDemandKafkaConsumer {
    private ObjectMapper mapper;
    private ServerProperties serverProperties;
    private OutboundRequestHandlerServiceImpl outboundRequestHandlerService;

    public VideoOnDemandKafkaConsumer(ServerProperties serverProperties,
                                      OutboundRequestHandlerServiceImpl outboundRequestHandlerService,
                                      ObjectMapper mapper) {
        this.serverProperties = serverProperties;
        this.outboundRequestHandlerService = outboundRequestHandlerService;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${spring.kafka.content.metadata.update.topic.name}", groupId = "${spring.kafka.content.metadata.update.consumer.group.id}")
    public void contentMetadataUpdateConsumerForVOD(ConsumerRecord<String, String> data) {
        log.debug("Processing Kafka message from topic: {}", data.topic());

        if (StringUtils.isBlank(data.value())) {
            log.warn("Received empty message from Kafka topic: {}", data.topic());
            return;
        }

        CompletableFuture.runAsync(() -> processMetadataUpdate(data.value()))
                .exceptionally(ex -> {
                    log.error("Failed to process metadata update asynchronously for topic: {}", data.topic(), ex);
                    return null;
                });
    }

    private void processMetadataUpdate(String messageValue) {
        try {
            Map<String, String> data = mapper.readValue(messageValue, new TypeReference<Map<String, String>>() {
            });

            if (!isValidRequest(data)) {
                log.warn("Invalid request data: missing identifier or streaming URL");
                return;
            }

            String identifier = data.get(Constants.IDENTIFIER);
            String streamingUrl = data.get(Constants.STREAMING_URI);

            updateContentMetadata(identifier, streamingUrl);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse Kafka message: {}", messageValue, e);
        } catch (Exception e) {
            log.error("Unexpected error processing metadata update for message: {}", messageValue, e);
        }
    }

    private boolean isValidRequest(Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return false;
        }

        String identifier = data.get(Constants.IDENTIFIER);
        String streamingUrl = data.get(Constants.STREAMING_URI);

        return StringUtils.isNotBlank(identifier) && StringUtils.isNotBlank(streamingUrl);
    }

    private void updateContentMetadata(String identifier, String streamingUrl) {
        log.debug("Updating content metadata for identifier: {}", identifier);

        try {
            String url = buildUpdateUrl(identifier);
            Map<String, String> headers = createHeaders();
            Map<String, Object> requestPayload = buildRequestPayload(streamingUrl);

            Map<String, Object> response = outboundRequestHandlerService.fetchResultUsingPatch(url, requestPayload, headers);

            handleUpdateResponse(identifier, response);

        } catch (Exception e) {
            log.error("Failed to update content metadata for identifier: {}", identifier, e);
        }
    }

    private String buildUpdateUrl(String identifier) {
        return serverProperties.getLearningServiceVmBaseUrl() +
                serverProperties.getSystemUpdateAPI() +
                identifier;
    }

    private Map<String, String> createHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
        return headers;
    }

    private Map<String, Object> buildRequestPayload(String streamingUrl) {
        String transformedUrl = streamingUrl.replaceAll(
                serverProperties.getVodBucketPrefix(),
                serverProperties.getVodStreamUrlPrefix()
        );

        Map<String, Object> content = new HashMap<>();
        content.put(Constants.STREAMING_URL, transformedUrl);

        Map<String, Object> contentRequest = new HashMap<>();
        contentRequest.put(Constants.CONTENT, content);

        Map<String, Object> request = new HashMap<>();
        request.put(Constants.REQUEST, contentRequest);

        return request;
    }

    private void handleUpdateResponse(String identifier, Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            log.warn("Received empty response for identifier: {}", identifier);
            return;
        }

        String responseCode = (String) response.get(Constants.RESPONSE_CODE);

        if (Constants.OK.equalsIgnoreCase(responseCode)) {
            log.info("Successfully updated metadata for identifier: {}", identifier);
        } else {
            log.warn("Failed to update metadata for identifier: {}, response code: {}", identifier, responseCode);
        }
    }
}
