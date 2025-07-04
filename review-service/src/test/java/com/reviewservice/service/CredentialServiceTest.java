package com.reviewservice.service;

import com.reviewcore.model.*;
import com.reviewservice.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.profiles.active=test")
@Import(TestConfig.class)
public class CredentialServiceTest {
    
    @Autowired
    private CredentialService credentialService;
    
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
} 