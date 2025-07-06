package com.reviewservice.service;

import com.reviewcore.model.*;
import com.reviewservice.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import java.util.Map;

import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.profiles.active=test")
@Import(TestConfig.class)
@Disabled("Disabled due to Flyway/JSONB incompatibility in H2")
public class CredentialServiceTest {
    
    private CredentialService credentialService;
    private EncryptionService encryptionService;

    @BeforeEach
    void setup() {
        encryptionService = Mockito.mock(EncryptionService.class);
        credentialService = new CredentialService(encryptionService);
    }
    
    @Test
    public void testBasicCredentialCreation() {
        BasicCredential credential = credentialService.createBasicCredential("testuser", "testpass");
        
        assertNotNull(credential);
        assertEquals("basic", credential.getType());
        assertEquals("testuser", credential.getUsername());
        assertEquals("testpass", credential.getPassword());
        assertTrue(credentialService.validateCredential(credential));
    }
    
    @Test
    public void testApiKeyCredentialCreation() {
        ApiKeyCredential credential = credentialService.createApiKeyCredential("test-api-key", "X-Custom-Header");
        
        assertNotNull(credential);
        assertEquals("apikey", credential.getType());
        assertEquals("test-api-key", credential.getApiKey());
        assertEquals("X-Custom-Header", credential.getHeaderName());
        assertTrue(credentialService.validateCredential(credential));
    }
    
    @Test
    public void testOAuthCredentialCreation() {
        OAuthCredential credential = credentialService.createOAuthCredential(
            "client-id", "client-secret", "access-token", "refresh-token", "https://token.url"
        );
        
        assertNotNull(credential);
        assertEquals("oauth", credential.getType());
        assertEquals("client-id", credential.getClientId());
        assertEquals("client-secret", credential.getClientSecret());
        assertEquals("access-token", credential.getAccessToken());
        assertEquals("refresh-token", credential.getRefreshToken());
        assertEquals("https://token.url", credential.getTokenUrl());
        assertTrue(credentialService.validateCredential(credential));
    }
    
    @Test
    public void testInvalidCredentialValidation() {
        assertFalse(credentialService.validateCredential(null));
        
        BasicCredential emptyBasic = new BasicCredential("", "");
        assertFalse(credentialService.validateCredential(emptyBasic));
        
        ApiKeyCredential emptyApiKey = new ApiKeyCredential("", "");
        assertFalse(credentialService.validateCredential(emptyApiKey));
    }

    @Test
    void testCreateAndValidateBasicCredential() {
        BasicCredential cred = credentialService.createBasicCredential("user", "pass");
        assertEquals("user", cred.getUsername());
        assertTrue(credentialService.validateCredential(cred));
        BasicCredential bad = credentialService.createBasicCredential("", "");
        assertFalse(credentialService.validateCredential(bad));
    }

    @Test
    void testCreateAndValidateApiKeyCredential() {
        ApiKeyCredential cred = credentialService.createApiKeyCredential("key", "header");
        assertEquals("key", cred.getApiKey());
        assertTrue(credentialService.validateCredential(cred));
        ApiKeyCredential bad = credentialService.createApiKeyCredential("", "header");
        assertFalse(credentialService.validateCredential(bad));
    }

    @Test
    void testCreateAndValidateOAuthCredential() {
        OAuthCredential cred = credentialService.createOAuthCredential("id", "secret", "token", "refresh", "url");
        assertEquals("id", cred.getClientId());
        assertTrue(credentialService.validateCredential(cred));
        OAuthCredential bad = credentialService.createOAuthCredential("", "", "", "", "");
        assertFalse(credentialService.validateCredential(bad));
    }

    @Test
    void testCreateAndValidateAwsCredential() {
        AwsCredential cred = credentialService.createAwsCredential("id", "secret");
        assertEquals("id", cred.getAccessKeyId());
        assertTrue(credentialService.validateCredential(cred));
        AwsCredential bad = credentialService.createAwsCredential("", "");
        assertFalse(credentialService.validateCredential(bad));
    }

    @Test
    void testGetAuthHeaders_basic() {
        BasicCredential cred = credentialService.createBasicCredential("user", "pass");
        Map<String, String> headers = credentialService.getAuthHeaders(cred);
        assertTrue(headers.containsKey("Authorization"));
    }

    @Test
    void testGetCredentialType() {
        BasicCredential cred = credentialService.createBasicCredential("user", "pass");
        assertEquals("basic", credentialService.getCredentialType(cred));
        assertNull(credentialService.getCredentialType(null));
    }

    @Test
    void testEncryptDecryptCredential() throws Exception {
        BasicCredential cred = credentialService.createBasicCredential("user", "pass");
        when(encryptionService.encrypt(anyString())).thenReturn("encrypted");
        when(encryptionService.decrypt(anyString())).thenReturn("{\"type\":\"basic\",\"username\":\"user\",\"password\":\"pass\"}");
        String encrypted = credentialService.encryptCredential(cred);
        assertEquals("encrypted", encrypted);
        Credential decrypted = credentialService.decryptCredential("encrypted");
        assertTrue(decrypted instanceof BasicCredential);
        assertEquals("user", ((BasicCredential) decrypted).getUsername());
    }
} 