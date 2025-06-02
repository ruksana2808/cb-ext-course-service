package com.igot.cb.access_settings.contoller;


import com.igot.cb.access_settings.service.AccessSettingsService;
import com.igot.cb.transactional.util.ApiResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.junit.jupiter.api.AfterEach;


import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AccessSettingsControllerTest {

  @InjectMocks
  private AccessSettingsController controller;

  @Mock
  private AccessSettingsService accessSettingsService;

  private AutoCloseable closeable;

  @BeforeEach
  void setUp() {
    closeable = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (closeable != null) {
      closeable.close();
    }
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
