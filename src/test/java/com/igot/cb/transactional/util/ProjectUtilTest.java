package com.igot.cb.transactional.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.exceptions.CustomException;
import com.igot.cb.exceptions.ResponseCode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProjectUtilTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ProjectUtil projectUtil;

    @Test
    public void testCreateDefaultResponse() {
        ApiResponse response = ProjectUtil.createDefaultResponse("test.api");

        Assert.assertEquals("test.api", response.getId());
        Assert.assertEquals(Constants.API_VERSION_1, response.getVer());
        Assert.assertEquals(Constants.SUCCESS, response.getParams().getStatus());
        Assert.assertEquals(HttpStatus.OK, response.getResponseCode());
        Assert.assertNotNull(response.getTs());
        Assert.assertNotNull(response.getParams().getResMsgId());
    }



}
