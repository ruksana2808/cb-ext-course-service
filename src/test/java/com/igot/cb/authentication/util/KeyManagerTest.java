package com.igot.cb.authentication.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.igot.cb.transactional.util.Constants;
import com.igot.cb.authentication.model.KeyData;

import com.igot.cb.transactional.util.PropertiesCache;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@RunWith(MockitoJUnitRunner.class)
public class KeyManagerTest {
    @InjectMocks
    private KeyManager keyManager;

    @Mock
    private PropertiesCache propertiesCache;

    private static final String TEMP_PUBLIC_KEY_FILE = "temp_public_key.pem";
    private Path tempDir;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("keymanager-test");
    }

    @After
    public void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @org.junit.Test
    public void testInit_shouldLoadPublicKeysSuccessfully() throws Exception {
        // Create dummy public key file
        String publicKeyContent = "-----BEGIN PUBLIC KEY-----\n" +
                Base64.getEncoder().encodeToString(generateTestKey().getEncoded()) + "\n" +
                "-----END PUBLIC KEY-----";
        Path pubKeyFile = tempDir.resolve(TEMP_PUBLIC_KEY_FILE);
        Files.write(pubKeyFile, publicKeyContent.getBytes(StandardCharsets.UTF_8));

        // Mock base path
        when(propertiesCache.getProperty(eq(Constants.ACCESS_TOKEN_PUBLICKEY_BASEPATH)))
                .thenReturn(tempDir.toString());

        // Call init
        keyManager.init();

        // Verify key is loaded
        KeyData keyData = keyManager.getPublicKey(TEMP_PUBLIC_KEY_FILE);
        Assert.assertNotNull(keyData);
        Assert.assertEquals(TEMP_PUBLIC_KEY_FILE, keyData.getKeyId());
        Assert.assertNotNull(keyData.getPublicKey());
    }

    @org.junit.Test
    public void testLoadPublicKey_shouldThrowExceptionOnInvalidKey() {
        String invalidKey = "-----BEGIN PUBLIC KEY-----\nInvalidKey\n-----END PUBLIC KEY-----";

        try {
            KeyManager.loadPublicKey(invalidKey);
            fail("Expected an exception due to invalid key");
        } catch (Exception e) {
            // success
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testGetPublicKey_shouldReturnNullWhenNotPresent() {
        assertNull(keyManager.getPublicKey("non-existent-key"));
    }

    private PublicKey generateTestKey() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair().getPublic();
    }

}