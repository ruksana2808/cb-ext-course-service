package com.igot.cb.service;

import java.util.Map;

import org.igot.common.ApiResponse;

public interface NotificationService {

    ApiResponse notifyAssignmentUploaded(Map<String, Object> requestData, String authToken);

    ApiResponse notifyAssignmentEvaluate(Map<String, Object> requestData, String authToken);

    ApiResponse notifyAssignmentSubmit(Map<String, Object> requestData, String authToken);

}
