package com.igot.cb.accessSettings.contoller;



import com.igot.cb.accessSettings.service.AccessSettingsService;
import com.igot.cb.transactional.util.ApiResponse;
import com.igot.cb.transactional.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccessSettingsControllerTest {

  @InjectMocks
  private AccessSettingsController controller;

  @Mock
  private AccessSettingsService accessSettingsService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testUpsert_ReturnsApiResponse() {
    Map<String, Object> userGroupDetails = new HashMap<>();
    String authToken = "dummy-token";
    ApiResponse mockResponse = new ApiResponse();
    mockResponse.setResponseCode(HttpStatus.OK);

    when(accessSettingsService.upsert(userGroupDetails, authToken)).thenReturn(mockResponse);

    ResponseEntity<ApiResponse> responseEntity = controller.upsert(userGroupDetails, authToken);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(mockResponse, responseEntity.getBody());
    verify(accessSettingsService, times(1)).upsert(userGroupDetails, authToken);
  }
}
