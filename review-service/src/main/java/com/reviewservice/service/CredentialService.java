package com.reviewservice.service;

import com.reviewcore.model.*;
import com.reviewservice.service.EncryptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.HashMap;

@Service
public class CredentialService {
    
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    public CredentialService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }
    
    /**
     * Creates a BasicCredential for username/password authentication
     */
    public BasicCredential createBasicCredential(String username, String password) {
        return new BasicCredential(username, password);
    }
    
    /**
     * Creates an ApiKeyCredential for API key authentication
     */
    public ApiKeyCredential createApiKeyCredential(String apiKey, String headerName) {
        return new ApiKeyCredential(apiKey, headerName != null ? headerName : "X-API-Key");
    }
    
    /**
     * Creates an OAuthCredential for OAuth authentication
     */
    public OAuthCredential createOAuthCredential(String clientId, String clientSecret, 
                                               String accessToken, String refreshToken, String tokenUrl) {
        return new OAuthCredential(clientId, clientSecret, accessToken, refreshToken, tokenUrl);
    }
    
    /**
     * Creates an AwsCredential for AWS S3 authentication
     */
    public AwsCredential createAwsCredential(String accessKeyId, String secretAccessKey) {
        return new AwsCredential(accessKeyId, secretAccessKey);
    }
    
    /**
     * Validates a credential based on its type
     */
    public boolean validateCredential(Credential credential) {
        if (credential == null) {
            return false;
        }
        
        switch (credential.getType()) {
            case "basic":
                BasicCredential basic = (BasicCredential) credential;
                return basic.getUsername() != null && !basic.getUsername().trim().isEmpty() &&
                       basic.getPassword() != null && !basic.getPassword().trim().isEmpty();
                       
            case "apikey":
                ApiKeyCredential apiKey = (ApiKeyCredential) credential;
                return apiKey.getApiKey() != null && !apiKey.getApiKey().trim().isEmpty();
                
            case "oauth":
                OAuthCredential oauth = (OAuthCredential) credential;
                return oauth.getClientId() != null && !oauth.getClientId().trim().isEmpty() &&
                       oauth.getClientSecret() != null && !oauth.getClientSecret().trim().isEmpty();
                       
            case "aws":
                AwsCredential aws = (AwsCredential) credential;
                return aws.getAccessKeyId() != null && !aws.getAccessKeyId().trim().isEmpty() &&
                       aws.getSecretAccessKey() != null && !aws.getSecretAccessKey().trim().isEmpty();
                       
            default:
                return false;
        }
    }
    
    /**
     * Gets authentication headers for HTTP requests based on credential type
     */
    public Map<String, String> getAuthHeaders(Credential credential) {
        Map<String, String> headers = new HashMap<>();
        
        if (credential == null) {
            return headers;
        }
        
        switch (credential.getType()) {
            case "basic":
                BasicCredential basic = (BasicCredential) credential;
                String auth = basic.getUsername() + ":" + basic.getPassword();
                String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
                headers.put("Authorization", "Basic " + encodedAuth);
                break;
                
            case "apikey":
                ApiKeyCredential apiKey = (ApiKeyCredential) credential;
                headers.put(apiKey.getHeaderName(), apiKey.getApiKey());
                break;
                
            case "oauth":
                OAuthCredential oauth = (OAuthCredential) credential;
                headers.put("Authorization", "Bearer " + oauth.getAccessToken());
                break;
                
            case "aws":
                // AWS credentials are typically used with AWS SDK, not HTTP headers
                // But we can set them as environment variables or use them directly
                AwsCredential aws = (AwsCredential) credential;
                headers.put("X-AWS-Access-Key-Id", aws.getAccessKeyId());
                headers.put("X-AWS-Secret-Access-Key", aws.getSecretAccessKey());
                break;
        }
        
        return headers;
    }
    
    /**
     * Gets the credential type as a string
     */
    public String getCredentialType(Credential credential) {
        return credential != null ? credential.getType() : null;
    }
    
    /**
     * Encrypts a credential to JSON string
     */
    public String encryptCredential(Credential credential) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(credential);
        return encryptionService.encrypt(json);
    }
    
    /**
     * Decrypts a credential from JSON string
     */
    public Credential decryptCredential(String encryptedJson) throws Exception {
        String decryptedJson = encryptionService.decrypt(encryptedJson);
        return objectMapper.readValue(decryptedJson, Credential.class);
    }
} 