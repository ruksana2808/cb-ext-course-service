package com.igot.cb.accessSettings.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.igot.cb.transactional.util.ApiResponse;
import java.util.Map;

public interface AccessSettingsService {

  ApiResponse upsert(Map<String, Object> userGroupDetails, String authToken);
}
