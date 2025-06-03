package com.igot.cb.exceptions;

import com.igot.cb.exceptions.CustomException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CustomExceptionTest {

    @org.junit.Test
    public void testDefaultConstructor() {
        CustomException exception = new CustomException();
        Assert.assertNull("Code should be null for default constructor", exception.getCode());
        Assert.assertNull("Message should be null for default constructor", exception.getMessage());
        Assert.assertNull("HttpStatusCode should be null for default constructor", exception.getHttpStatusCode());
    }

    @org.junit.Test
    public void testParameterizedConstructor() {
        String code = "ERR_001";
        String message = "An error occurred";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        CustomException exception = new CustomException(code, message, status);
        Assert.assertEquals("Code should match the provided value", code, exception.getCode());
        Assert.assertEquals("Message should match the provided value", message, exception.getMessage());
        Assert.assertEquals("HttpStatusCode should match the provided value", status, exception.getHttpStatusCode());
    }

    @org.junit.Test
    public void testSettersAndGetters() {
        CustomException exception = new CustomException();
        String code = "ERR_002";
        String message = "Another error occurred";
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        exception.setCode(code);
        exception.setMessage(message);
        exception.setHttpStatusCode(status);
        Assert.assertEquals("Code should be updated by setter", code, exception.getCode());
        Assert.assertEquals("Message should be updated by setter", message, exception.getMessage());
        Assert.assertEquals("HttpStatusCode should be updated by setter", status, exception.getHttpStatusCode());
    }

    @org.junit.Test
    public void testInheritanceFromRuntimeException() {
        CustomException exception = new CustomException();
        assertTrue("CustomException should be an instance of RuntimeException",
                exception instanceof RuntimeException);
    }

    @org.junit.Test
    public void testWithNullValues() {
        CustomException exception = new CustomException(null, null, null);
        Assert.assertNull("Code should be null when initialized with null", exception.getCode());
        Assert.assertNull("Message should be null when initialized with null", exception.getMessage());
        Assert.assertNull("HttpStatusCode should be null when initialized with null", exception.getHttpStatusCode());
    }

    @org.junit.Test
    public void testConstructorWithResponseCode() {
        String code = "ERR_003";
        String message = "Error with response code";
        int responseCode = 400;
        CustomException exception = new CustomException(code, message, responseCode);
        Assert.assertEquals("Code should match the provided value", code, exception.getCode());
        Assert.assertEquals("Message should match the provided value", message, exception.getMessage());
        Assert.assertEquals("ResponseCode should match the provided value", responseCode, exception.getResponseCode());
        Assert.assertNull("HttpStatusCode should be null when using responseCode constructor",
                exception.getHttpStatusCode());
    }

    @Test
    public void testConstructorWithHttpStatusAndErrorCode() {
        String code = "ACCESS_DENIED";
        String message = "User does not have sufficient permissions";
        HttpStatus httpStatus = HttpStatus.FORBIDDEN;
        String errorCode = "ERR_403";
        CustomException exception = new CustomException(code, message, httpStatus, errorCode);
        Assert.assertEquals("Code should match the provided value", code, exception.getCode());
        Assert.assertEquals("Message should match the provided value", message, exception.getMessage());
        Assert.assertEquals("HttpStatusCode should match the provided value", httpStatus, exception.getHttpStatusCode());
        Assert.assertEquals("ErrorCode should match the provided value", errorCode, exception.getErrorCode());
        Assert.assertEquals("ResponseCode should be 0 by default", 0, exception.getResponseCode());
    }

}
