package com.igot.cb.transactional.util;

import java.io.IOException;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProjectUtil {

    @Autowired
    private ObjectMapper mapper;

    public static ApiResponse createDefaultResponse(String api) {
        ApiResponse response = new ApiResponse();
        response.setId(api);
        response.setVer(Constants.API_VERSION_1);
        response.setParams(new ApiRespParam(UUID.randomUUID().toString()));
        response.getParams().setStatus(Constants.SUCCESS);
        response.setResponseCode(HttpStatus.OK);
        response.setTs(DateTime.now().toString());
        return response;
    }

    public String convertToString(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (IOException e) {
            log.error("Error converting object to string: {}", e.getMessage(), e);
            return null;
        }
    }
}
