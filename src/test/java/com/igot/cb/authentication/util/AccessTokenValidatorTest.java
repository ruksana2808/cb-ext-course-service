package com.igot.cb.authentication.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.transactional.util.Constants;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.KeyWrapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.PublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessTokenValidatorTest {

    @Mock
    private KeyManager keyManager;

    @Mock
    private KeyWrapper mockKeyWrapper;

    @Mock
    private PublicKey mockPublicKey;

    @InjectMocks
    private AccessTokenValidator accessTokenValidator;

    @Spy
    private AccessTokenValidator spyAccessTokenValidator;

    private static final ObjectMapper mapper = new ObjectMapper();

    private String validToken;
    private String expiredToken;
    private String invalidSignatureToken;
    private String invalidIssuerToken;

    @BeforeEach
    void setUp() throws Exception {
        // Mock PropertiesCache.getInstance().getProperty(...) if needed
        // Generate tokens for different scenarios
        validToken = generateToken("validUserId", Time.currentTime() + 1000, "expectedIssuer");
        expiredToken = generateToken("expiredUserId", Time.currentTime() - 1000, "expectedIssuer");
        invalidSignatureToken = generateToken("invalidSignatureUserId", Time.currentTime() + 1000, "expectedIssuer");
        invalidIssuerToken = generateToken("invalidIssuerUserId", Time.currentTime() + 1000, "invalidIssuer");

    }

    @Test
    void testVerifyUserToken_ExpiredToken() {
        String userId = accessTokenValidator.verifyUserToken(expiredToken);
        assertEquals(Constants.UNAUTHORIZED, userId);
    }

    @Test
    void testVerifyUserToken_InvalidSignature() {
        String userId = accessTokenValidator.verifyUserToken(invalidSignatureToken);
        assertEquals(Constants.UNAUTHORIZED, userId);
    }

    @Test
    void testVerifyUserToken_InvalidIssuer() {
        String userId = accessTokenValidator.verifyUserToken(invalidIssuerToken);
        assertEquals(Constants.UNAUTHORIZED, userId);
    }

    @Test
    void testFetchUserIdFromAccessToken_NullToken() {
        String userId = accessTokenValidator.fetchUserIdFromAccessToken(null);
        assertNull(userId);
    }

    // Helper method to generate tokens
    private String generateToken(String userId, int exp, String issuer) throws Exception {
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "RS256");
        header.put("typ", "JWT");
        header.put("kid", "testKeyId");

        Map<String, Object> body = new HashMap<>();
        body.put("sub", "user:" + userId);
        body.put("exp", exp);
        body.put("iss", issuer);

        String headerJson = mapper.writeValueAsString(header);
        String bodyJson = mapper.writeValueAsString(body);

        String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes());
        String encodedBody = Base64.getUrlEncoder().withoutPadding().encodeToString(bodyJson.getBytes());

        String unsignedToken = encodedHeader + "." + encodedBody;

        // For simplicity, we're not signing the token here
        String signature = "testSignature";

        return unsignedToken + "." + signature;
    }

    @Test
    void fetchUserIdFromAccessToken_validToken_returnsUserId() throws Exception {
        String accessToken = "validToken";
        String expectedUserId = "user123";


        doReturn(expectedUserId).when(spyAccessTokenValidator).verifyUserToken(accessToken);

        String actualUserId = spyAccessTokenValidator.fetchUserIdFromAccessToken(accessToken);

        assertEquals(expectedUserId, actualUserId);
    }

    @Test
    void fetchUserIdFromAccessToken_unauthorizedToken_returnsNull() throws Exception {
        String accessToken = "unauthorizedToken";

        doReturn("UNAUTHORIZED").when(spyAccessTokenValidator).verifyUserToken(accessToken);

        String actualUserId = spyAccessTokenValidator.fetchUserIdFromAccessToken(accessToken);

        assertNull(actualUserId);
    }

    @Test
    void fetchUserIdFromAccessToken_nullToken_returnsNull() {
        String actualUserId = spyAccessTokenValidator.fetchUserIdFromAccessToken(null);
        assertNull(actualUserId);
    }

    @Test
    void fetchUserIdFromAccessToken_exceptionThrown_returnsNull() throws Exception {
        String accessToken = "token";

        doThrow(new RuntimeException("some error")).when(spyAccessTokenValidator).verifyUserToken(accessToken);

        String actualUserId = spyAccessTokenValidator.fetchUserIdFromAccessToken(accessToken);

        assertNull(actualUserId);
    }
}
