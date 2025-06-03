package com.igot.cb.access_settings.service.impl;



import com.igot.cb.access_settings.util.Constants;
import com.igot.cb.access_settings.util.PayloadValidation;
import com.igot.cb.transactional.cassandrautils.CassandraOperation;
import com.igot.cb.transactional.util.ApiResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccessSettingsServiceImplTest {

  @Mock
  private PayloadValidation payloadValidation;

  @Mock
  private CassandraOperation cassandraOperation;

  private AccessSettingsServiceImpl service;

  private AutoCloseable mocks;

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    service = new AccessSettingsServiceImpl(cassandraOperation, payloadValidation);
  }

  @AfterEach
  void tearDown() throws Exception {
    mocks.close();
  }


  @Test
  void testUpsert_NullUserGroupDetails() {
    ApiResponse response = service.upsert(null, "token");
    assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    assertEquals(Constants.FAILED, response.getParams().getStatus());
    assertTrue(response.getParams().getErrMsg().contains("cannot be null or empty"));
  }

  @Test
  void testUpsert_EmptyUserGroupDetails() {
    ApiResponse response = service.upsert(new HashMap<>(), "token");
    assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    assertEquals(Constants.FAILED, response.getParams().getStatus());
    assertTrue(response.getParams().getErrMsg().contains("cannot be null or empty"));
  }

  @Test
  void testUpsert_ValidationError() {
    // Make details non-empty so validation is triggered
    Map<String, Object> details = new HashMap<>();
    details.put(Constants.CONTENT_ID, "cid"); // or any dummy key
    when(payloadValidation.validateAccessControlPayload(details)).thenReturn("validation error");
    ApiResponse response = service.upsert(details, "token");
    assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    assertEquals(Constants.FAILED, response.getParams().getStatus());
    assertTrue(response.getParams().getErrMsg().contains("validation error"));
  }

  @Test
  void testUpsert_Success() {
    Map<String, Object> details = new HashMap<>();
    details.put(Constants.CONTENT_ID, "cid");
    details.put(Constants.ACCESS_CONTROL, new HashMap<>());
    when(payloadValidation.validateAccessControlPayload(details)).thenReturn("");
    // insertRecord likely returns void or null; adjust as needed
    when(cassandraOperation.insertRecord(anyString(), anyString(), anyMap())).thenReturn(null);

    ApiResponse response = service.upsert(details, "token");
    assertEquals(HttpStatus.OK, response.getResponseCode());
    assertEquals(Constants.CREATED_RULES, response.getResult().get(Constants.MSG));
    assertNotNull(response.getResult().get(Constants.DATA));
  }

  @Test
  void testUpsert_Exception() {
    Map<String, Object> details = new HashMap<>();
    details.put(Constants.CONTENT_ID, "cid");
    details.put(Constants.ACCESS_CONTROL, new HashMap<>());
    when(payloadValidation.validateAccessControlPayload(details)).thenReturn("");
    doThrow(new RuntimeException("db error")).when(cassandraOperation).insertRecord(anyString(), anyString(), anyMap());

    ApiResponse response = service.upsert(details, "token");
    assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    assertEquals(Constants.FAILED, response.getParams().getStatus());
    assertTrue(response.getParams().getErrMsg().contains("Failed to create access settings"));
  }

  @SuppressWarnings("unchecked")
  @Test
  void testCreateUserGroupIds_AddsUuid() {
    Map<String, Object> userGroup = new HashMap<>();
    userGroup.put(Constants.USER_GROUP_ID, null);
    List<Map<String, Object>> userGroups = new ArrayList<>();
    userGroups.add(userGroup);
    Map<String, Object> accessControl = new HashMap<>();
    accessControl.put(Constants.USER_GROUPS, userGroups);
    Map<String, Object> payload = new HashMap<>();
    payload.put(Constants.ACCESS_CONTROL, accessControl);

    Map<String, Object> result = service.createUserGroupIds(payload);
    List<Map<String, Object>> resultGroups = (List<Map<String, Object>>)
            ((Map<String, Object>) result.get(Constants.ACCESS_CONTROL)).get(Constants.USER_GROUPS);
    assertNotNull(resultGroups.get(0).get(Constants.USER_GROUP_ID));
  }

  @SuppressWarnings("unchecked")
  @Test
  void testCreateUserGroupIds_WithExistingId() {
    Map<String, Object> userGroup = new HashMap<>();
    userGroup.put(Constants.USER_GROUP_ID, "existing-id");
    List<Map<String, Object>> userGroups = new ArrayList<>();
    userGroups.add(userGroup);
    Map<String, Object> accessControl = new HashMap<>();
    accessControl.put(Constants.USER_GROUPS, userGroups);
    Map<String, Object> payload = new HashMap<>();
    payload.put(Constants.ACCESS_CONTROL, accessControl);

    Map<String, Object> result = service.createUserGroupIds(payload);
    List<Map<String, Object>> resultGroups = (List<Map<String, Object>>)
            ((Map<String, Object>) result.get(Constants.ACCESS_CONTROL)).get(Constants.USER_GROUPS);
    assertEquals("existing-id", resultGroups.get(0).get(Constants.USER_GROUP_ID));
  }

  @Test
  void testCreateUserGroupIds_NoUserGroups() {
    Map<String, Object> accessControl = new HashMap<>();
    Map<String, Object> payload = new HashMap<>();
    payload.put(Constants.ACCESS_CONTROL, accessControl);

    Map<String, Object> result = service.createUserGroupIds(payload);
    assertEquals(accessControl, result.get(Constants.ACCESS_CONTROL));
  }

  @Test
  void testCreateUserGroupIds_NoAccessControl() {
    Map<String, Object> payload = new HashMap<>();
    Map<String, Object> result = service.createUserGroupIds(payload);
    assertEquals(payload, result);
  }
}

