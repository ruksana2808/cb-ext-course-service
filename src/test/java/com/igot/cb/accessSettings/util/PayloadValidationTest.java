package com.igot.cb.accessSettings.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class PayloadValidationTest {

  private PayloadValidation payloadValidation;

  @Before
  public void setUp() {
    payloadValidation = new PayloadValidation();
  }

  @Test
  public void testValidPayload() {
    Map<String, Object> payload = new HashMap<>();
    payload.put(Constants.CONTENT_ID, "content123");

    Map<String, Object> accessControl = new HashMap<>();
    List<Map<String, Object>> userGroups = new ArrayList<>();
    Map<String, Object> userGroup = new HashMap<>();
    List<Map<String, Object>> criteriaList = new ArrayList<>();
    Map<String, Object> criteria = new HashMap<>();
    criteria.put(Constants.CRITERIA_VALUE, Arrays.asList("val1"));
    criteriaList.add(criteria);
    userGroup.put(Constants.USER_GROUP_CRTIRIA_LIST, criteriaList);
    userGroups.add(userGroup);
    accessControl.put(Constants.USER_GROUPS, userGroups);

    payload.put(Constants.ACCESS_CONTROL, accessControl);

    String result = payloadValidation.validateAccessControlPayload(payload);
    assertTrue(result.isEmpty());
  }

  @Test
  public void testMissingContentId() {
    Map<String, Object> payload = new HashMap<>();
    payload.put(Constants.ACCESS_CONTROL, new HashMap<>());

    String result = payloadValidation.validateAccessControlPayload(payload);
    assertTrue(result.contains("contentId"));
  }

  @Test
  public void testBlankContentId() {
    Map<String, Object> payload = new HashMap<>();
    payload.put(Constants.CONTENT_ID, "   ");
    payload.put(Constants.ACCESS_CONTROL, new HashMap<>());

    String result = payloadValidation.validateAccessControlPayload(payload);
    assertTrue(result.contains("contentId"));
  }

  @Test
  public void testMissingAccessControl() {
    Map<String, Object> payload = new HashMap<>();
    payload.put(Constants.CONTENT_ID, "content123");

    String result = payloadValidation.validateAccessControlPayload(payload);
    assertTrue(result.contains(Constants.ACCESS_CONTROL));
  }

  @Test
  public void testInvalidAccessControlType() {
    Map<String, Object> payload = new HashMap<>();
    payload.put(Constants.CONTENT_ID, "content123");
    payload.put(Constants.ACCESS_CONTROL, "notAMap");

    String result = payloadValidation.validateAccessControlPayload(payload);
    assertTrue(result.contains(Constants.ACCESS_CONTROL));
  }

  @Test
  public void testMissingUserGroups() {
    Map<String, Object> payload = new HashMap<>();
    payload.put(Constants.CONTENT_ID, "content123");
    Map<String, Object> accessControl = new HashMap<>();
    payload.put(Constants.ACCESS_CONTROL, accessControl);

    String result = payloadValidation.validateAccessControlPayload(payload);
    // Should expect an error for missing accessControl (empty map)
    assertTrue(result.contains(Constants.ACCESS_CONTROL));
  }

  @Test
  public void testEmptyCriteriaValue() {
    Map<String, Object> payload = new HashMap<>();
    payload.put(Constants.CONTENT_ID, "content123");

    Map<String, Object> accessControl = new HashMap<>();
    List<Map<String, Object>> userGroups = new ArrayList<>();
    Map<String, Object> userGroup = new HashMap<>();
    List<Map<String, Object>> criteriaList = new ArrayList<>();
    Map<String, Object> criteria = new HashMap<>();
    criteria.put(Constants.CRITERIA_VALUE, new ArrayList<>());
    criteriaList.add(criteria);
    userGroup.put(Constants.USER_GROUP_CRTIRIA_LIST, criteriaList);
    userGroups.add(userGroup);
    accessControl.put(Constants.USER_GROUPS, userGroups);

    payload.put(Constants.ACCESS_CONTROL, accessControl);

    String result = payloadValidation.validateAccessControlPayload(payload);
    assertTrue(result.contains("criteriaValue"));
  }
}