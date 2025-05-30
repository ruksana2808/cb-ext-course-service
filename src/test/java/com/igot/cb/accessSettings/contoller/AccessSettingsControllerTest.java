package com.igot.cb.accessSettings.contoller;



import com.igot.cb.accessSettings.service.AccessSettingsService;
import com.igot.cb.transactional.util.ApiResponse;
import com.igot.cb.transactional.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccessSettingsControllerTest {

    @InjectMocks
    private AccessSettingsController controller;

    @Mock
    private AccessSettingsService accessSettingsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void upsert_shouldReturnApiResponse() {
        Map<String, Object> userGroupDetails = new HashMap<>();
        userGroupDetails.put("key", "value");
        String authToken = "test-token";
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setResponseCode(HttpStatus.OK);

        when(accessSettingsService.upsert(userGroupDetails, authToken)).thenReturn(apiResponse);

        ResponseEntity<ApiResponse> response = controller.upsert(userGroupDetails, authToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(apiResponse, response.getBody());
        verify(accessSettingsService, times(1)).upsert(userGroupDetails, authToken);
    }

    @Test
    void upsert_endpoint_shouldReturn200() throws Exception {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setResponseCode(HttpStatus.OK);

        when(accessSettingsService.upsert(anyMap(), anyString())).thenReturn(apiResponse);

        mockMvc.perform(put("/accessSetttings/v1/upsert")
                        .header(Constants.X_AUTH_TOKEN, "token")
                        .contentType("application/json")
                        .content("{\"key\":\"value\"}"))
                .andExpect(status().isOk());
    }
}
