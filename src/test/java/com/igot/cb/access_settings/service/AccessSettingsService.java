package com.igot.cb.access_settings.service;

import com.igot.cb.transactional.util.ApiResponse;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

class AccessSettingsServiceTest {

  @Test
  void testUpsertMethodIsCalled() {
    AccessSettingsService service = mock(AccessSettingsService.class);
    Map<String, Object> details = new HashMap<>();
    String token = "test-token";
    ApiResponse response = new ApiResponse();

    when(service.upsert(details, token)).thenReturn(response);

    service.upsert(details, token);

    verify(service, times(1)).upsert(details, token);
  }
}
