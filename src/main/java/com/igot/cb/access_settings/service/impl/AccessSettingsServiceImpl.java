package com.igot.cb.access_settings.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.access_settings.service.AccessSettingsService;
import com.igot.cb.access_settings.util.Constants;
import com.igot.cb.access_settings.util.PayloadValidation;
import com.igot.cb.transactional.cassandrautils.CassandraOperation;
import com.igot.cb.transactional.util.ApiResponse;
import com.igot.cb.transactional.util.ProjectUtil;

import java.util.*;

import com.igot.cb.transactional.util.exceptions.CustomException;
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

  private final Logger logger = LoggerFactory.getLogger(AccessSettingsServiceImpl.class);

  private final PayloadValidation payloadValidation;
  private final CassandraOperation cassandraOperation;

  @Autowired
  public AccessSettingsServiceImpl(CassandraOperation cassandraOperation, PayloadValidation payloadValidation) {
    this.cassandraOperation = cassandraOperation;
    this.payloadValidation = payloadValidation;
  }

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
      logger.error("Error while upserting access settings", e);
      setFailedResponse(response, "Failed to create access settings: " + e.getMessage());
      return response;
    }
  }

  @Override
  public ApiResponse read(String contentId) {
    logger.info("AccessSettingsService::read:inside");
    ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_ACCESS_RULE_READ);
    try {
      Map<String, Object> propertyMap = new HashMap<>();
      propertyMap.put(Constants.CONTEXT_ID, contentId);
      List<String> fields = new ArrayList<>();
      fields.add(Constants.CONTEXT_ID);
      fields.add(Constants.CONTEXT_DATA);
      fields.add(Constants.IS_ARCHIVED);
      List<Map<String, Object>> accessSettingRule = cassandraOperation.getRecordsByPropertiesWithoutFiltering(
              Constants.KEYSPACE_SUNBIRD_COURSE, Constants.ACCESS_SETTINGS_RULES_TABLE, propertyMap,
              fields, null);
      if (!accessSettingRule.isEmpty()) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Map<String, Object> record : accessSettingRule) {
          Boolean status = (Boolean) record.get(Constants.IS_ARCHIVED);
          if (Boolean.FALSE.equals(status)) {
            String contextDataJson = (String) record.get(Constants.CONTEXT_DATA);
            try {
              Map<String, Object> contextDataMap = objectMapper.readValue(
                      contextDataJson, new TypeReference<Map<String, Object>>() {});
              // Add contextid and isarchived to the map
              contextDataMap.put(Constants.CONTEXT_ID, record.get(Constants.CONTEXT_ID));
              contextDataMap.put(Constants.IS_ARCHIVED, status);
              resultList.add(contextDataMap);
            } catch (Exception e) {
              logger.error("Failed to parse CONTEXT_DATA JSON", e);
              throw new CustomException(
                      Constants.ERROR,
                      "error while processing",
                      HttpStatus.INTERNAL_SERVER_ERROR
              );
            }
          }
        }
        response.getResult().put(Constants.DATA, resultList);
        return response;
      }
      response.getParams().setStatus(Constants.FAILED);
      response.setResponseCode(HttpStatus.NOT_FOUND);
      response.getParams().setErrMsg("No access settings found for the given contentId");
      return response;

    } catch (Exception e) {
      logger.error("Error while joining community:", e);
      throw new CustomException(
              Constants.ERROR,
              "error while processing",
              HttpStatus.INTERNAL_SERVER_ERROR
      );
    }
  }

  private void setFailedResponse(ApiResponse response, String errorMessage) {
    response.getParams().setStatus(Constants.FAILED);
    response.setResponseCode(HttpStatus.BAD_REQUEST);
    response.getParams().setErrMsg(errorMessage);
  }

  @SuppressWarnings("unchecked")
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
