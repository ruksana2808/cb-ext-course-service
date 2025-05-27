package com.igot.cb.accessSettings.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.igot.cb.accessSettings.service.AccessSettingsService;
import com.igot.cb.accessSettings.util.Constants;
import com.igot.cb.accessSettings.util.PayloadValidation;
import com.igot.cb.transactional.util.ApiResponse;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AccessSettingsServiceImpl implements AccessSettingsService {

  private Logger logger = LoggerFactory.getLogger(AccessSettingsServiceImpl.class);

  @Autowired
  private PayloadValidation payloadValidation;

  @Override
    public ApiResponse upsert(Map<String, Object> userGroupDetails, String authToken) {
    logger.info("AccessSettingsService::create:inside");
    return null;
  }

}
