package com.igot.cb.transactional.util;

import com.igot.cb.transactional.util.ApiRespParam;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ApiRespParamTest {

    @org.junit.Test
    public void testDefaultConstructor() {
        ApiRespParam params = new ApiRespParam();
        Assert.assertNull("ResMsgId should be null for default constructor", params.getResMsgId());
        Assert.assertNull("MsgId should be null for default constructor", params.getMsgId());
        Assert.assertNull("Err should be null for default constructor", params.getErr());
        Assert.assertNull("Status should be null for default constructor", params.getStatus());
        Assert.assertNull("ErrMsg should be null for default constructor", params.getErrMsg());
    }

    @org.junit.Test
    public void testParameterizedConstructor() {
        String id = UUID.randomUUID().toString();
        ApiRespParam params = new ApiRespParam(id);
        Assert.assertEquals("ResMsgId should match the provided ID", id, params.getResMsgId());
        Assert.assertEquals("MsgId should match the provided ID", id, params.getMsgId());
        Assert.assertNull("Err should be null initially", params.getErr());
        Assert.assertNull("Status should be null initially", params.getStatus());
        Assert.assertNull("ErrMsg should be null initially", params.getErrMsg());
    }

    @org.junit.Test
    public void testSettersAndGetters() {
        ApiRespParam params = new ApiRespParam();
        String resMsgId = "res-123";
        String msgId = "msg-456";
        String err = "ERR_001";
        String status = "FAILED";
        String errMsg = "An error occurred";
        params.setResMsgId(resMsgId);
        params.setMsgId(msgId);
        params.setErr(err);
        params.setStatus(status);
        params.setErrMsg(errMsg);
        Assert.assertEquals("ResMsgId should be updated", resMsgId, params.getResMsgId());
        Assert.assertEquals("MsgId should be updated", msgId, params.getMsgId());
        Assert.assertEquals("Err should be updated", err, params.getErr());
        Assert.assertEquals("Status should be updated", status, params.getStatus());
        Assert.assertEquals("ErrMsg should be updated", errMsg, params.getErrMsg());
    }

    @org.junit.Test
    public void testWithNullValues() {
        ApiRespParam params = new ApiRespParam("test-id");
        params.setResMsgId(null);
        params.setMsgId(null);
        params.setErr(null);
        params.setStatus(null);
        params.setErrMsg(null);
        Assert.assertNull("ResMsgId should be set to null", params.getResMsgId());
        Assert.assertNull("MsgId should be set to null", params.getMsgId());
        Assert.assertNull("Err should be set to null", params.getErr());
        Assert.assertNull("Status should be set to null", params.getStatus());
        Assert.assertNull("ErrMsg should be set to null", params.getErrMsg());
    }

    @Test
    public void testEmptyStringValues() {
        ApiRespParam params = new ApiRespParam();
        String emptyString = "";
        params.setResMsgId(emptyString);
        params.setMsgId(emptyString);
        params.setErr(emptyString);
        params.setStatus(emptyString);
        params.setErrMsg(emptyString);
        Assert.assertEquals("ResMsgId should be set to empty string", emptyString, params.getResMsgId());
        Assert.assertEquals("MsgId should be set to empty string", emptyString, params.getMsgId());
        Assert.assertEquals("Err should be set to empty string", emptyString, params.getErr());
        Assert.assertEquals("Status should be set to empty string", emptyString, params.getStatus());
        Assert.assertEquals("ErrMsg should be set to empty string", emptyString, params.getErrMsg());
    }
}
