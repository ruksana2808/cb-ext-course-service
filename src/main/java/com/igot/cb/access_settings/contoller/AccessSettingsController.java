package com.igot.cb.access_settings.contoller;

import com.igot.cb.access_settings.service.AccessSettingsService;
import com.igot.cb.transactional.util.ApiResponse;
import com.igot.cb.transactional.util.Constants;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accessSetttings")
public class AccessSettingsController {

  @SuppressWarnings("unused")
  @Autowired
  private AccessSettingsService accessSettingsService;

  //createand update API
  @PutMapping("/v1/upsert")
  public ResponseEntity<ApiResponse> upsert(@RequestBody Map<String, Object> userGroupDetails,
      @RequestHeader(Constants.X_AUTH_TOKEN) String authToken) {
    ApiResponse response = accessSettingsService.upsert(userGroupDetails, authToken);
    return new ResponseEntity<>(response, response.getResponseCode());
  }

}
