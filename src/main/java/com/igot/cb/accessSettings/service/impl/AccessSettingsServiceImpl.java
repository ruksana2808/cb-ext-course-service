package com.igot.cb.accessSettings.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.accessSettings.service.AccessSettingsService;
import com.igot.cb.accessSettings.util.Constants;
import com.igot.cb.accessSettings.util.PayloadValidation;
import com.igot.cb.transactional.cassandrautils.CassandraOperation;
import com.igot.cb.transactional.util.ApiResponse;
import com.igot.cb.transactional.util.ProjectUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AccessSettingsServiceImpl implements AccessSettingsService {

  private Logger logger = LoggerFactory.getLogger(AccessSettingsServiceImpl.class);

  @Autowired
  private PayloadValidation payloadValidation;

  @Autowired
  CassandraOperation cassandraOperation;

  ObjectMapper objectMapper = new ObjectMapper();

  @Override
    public ApiResponse upsert(Map<String, Object> userGroupDetails, String authToken) {
    logger.info("AccessSettingsService::create:inside");
    ApiResponse response = ProjectUtil.createDefaultResponse(Constants.ACCESS_SETTINGS_CREATE_API);
    if (userGroupDetails == null || userGroupDetails.isEmpty()) {
      logger.error("User group details are null or empty");
      setFailedResponse(response, "User group details cannot be null or empty");
      return response;
    }
    String errMsg = payloadValidation.validateAccessControlPayload(userGroupDetails);
    if (StringUtils.isNotBlank(errMsg)) {
      setFailedResponse(response, errMsg);
      return response;
    }
    try {
      Map<String, Object> createPayloadWithUuid = createUserGroupIds(userGroupDetails);
      Map<String, Object> accessRuleData = new HashMap<>();
      String accessRuleDataJson = objectMapper.writeValueAsString(createPayloadWithUuid);
      accessRuleData.put(Constants.CONTEXT_ID, userGroupDetails.get(Constants.CONTENT_ID));
      accessRuleData.put(Constants.CONTEXT_DATA, accessRuleDataJson);
      accessRuleData.put(Constants.IS_ARCHIVED, false);
      cassandraOperation.insertRecord(Constants.KEYSPACE_SUNBIRD_COURSE,
          Constants.ACCESS_SETTINGS_RULES_TABLE, accessRuleData);
      response.getResult().put(Constants.MSG, Constants.CREATED_RULES);
      response.getResult().put(Constants.DATA, createPayloadWithUuid);
      return response;
    } catch (Exception e) {
      logger.error("Error while upserting access settings: {}",e);
      setFailedResponse(response, "Failed to create access settings: " + e.getMessage());
      return response;
    }
  }

  private void setFailedResponse(ApiResponse response, String errorMessage) {
    response.getParams().setStatus(Constants.FAILED);
    response.setResponseCode(HttpStatus.BAD_REQUEST);
    response.getParams().setErrMsg(errorMessage);
  }

  public Map<String, Object> createUserGroupIds(Map<String, Object> payload) {
    Object accessControlObj = payload.get(Constants.ACCESS_CONTROL);
    if (accessControlObj instanceof Map) {
      Map<String, Object> accessControl = (Map<String, Object>) accessControlObj;
      Object userGroupsObj = accessControl.get(Constants.USER_GROUPS);
      if (userGroupsObj instanceof List) {
        List<Map<String, Object>> userGroups = (List<Map<String, Object>>) userGroupsObj;
        for (Map<String, Object> userGroup : userGroups) {
          Object id = userGroup.get(Constants.USER_GROUP_ID);
          if (id == null || id.toString().trim().isEmpty()) {
            userGroup.put(Constants.USER_GROUP_ID, UUID.randomUUID().toString());
          }
        }
      }
    }
    return payload;
  }
}
