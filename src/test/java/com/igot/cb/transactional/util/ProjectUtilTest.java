package com.igot.cb.transactional.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ProjectUtilTest {

    @Test
    void testCreateDefaultResponse() {
        String apiName = "testApi";
        ApiResponse response = ProjectUtil.createDefaultResponse(apiName);

        assertNotNull(response);
        assertEquals(apiName, response.getId());
        assertEquals(Constants.API_VERSION_1, response.getVer());
        assertNotNull(response.getParams());
        assertNotNull(response.getParams().getResMsgId());
        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertNotNull(response.getTs());
    }
}
