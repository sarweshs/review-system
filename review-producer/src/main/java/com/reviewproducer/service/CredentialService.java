package com.reviewproducer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewcore.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
public class CredentialService {
    
    private final VaultService vaultService;
    private final ObjectMapper objectMapper;
    
    public CredentialService(VaultService vaultService, ObjectMapper objectMapper) {
        this.vaultService = vaultService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Decrypts a credential from encrypted JSON string
     */
    public Credential decryptCredential(String encryptedJson) throws Exception {
        if (encryptedJson == null || encryptedJson.trim().isEmpty()) {
            log.warn("Encrypted JSON is null or empty");
            return null;
        }
        
        try {
            String decryptedJson = vaultService.decrypt(encryptedJson);
            if (decryptedJson == null || decryptedJson.trim().isEmpty()) {
                log.error("Failed to decrypt credential - decrypted result is null or empty");
                throw new RuntimeException("Failed to decrypt credential - decrypted result is null or empty");
            }
            
            Credential credential = objectMapper.readValue(decryptedJson, Credential.class);
            if (credential == null) {
                log.error("Failed to deserialize credential from JSON");
                throw new RuntimeException("Failed to deserialize credential from JSON");
            }
            
            // Validate credential type
            if (credential.getType() == null || credential.getType().trim().isEmpty()) {
                log.error("Credential type is null or empty");
                throw new RuntimeException("Credential type is null or empty");
            }
            
            // Validate credential based on type
            validateCredential(credential);
            
            log.debug("Successfully decrypted and validated credential of type: {}", credential.getType());
            return credential;
            
        } catch (Exception e) {
            log.error("Failed to decrypt credential - {}", e.getMessage(), e);
            throw new RuntimeException("Failed to decrypt credential: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validates credential based on its type
     */
    private void validateCredential(Credential credential) {
        String type = credential.getType();
        
        switch (type) {
            case "basic":
                validateBasicCredential((BasicCredential) credential);
                break;
            case "apikey":
                validateApiKeyCredential((ApiKeyCredential) credential);
                break;
            case "oauth":
                validateOAuthCredential((OAuthCredential) credential);
                break;
            case "aws":
                validateAwsCredential((AwsCredential) credential);
                break;
            default:
                log.error("Unknown credential type: {}", type);
                throw new RuntimeException("Unknown credential type: " + type);
        }
    }
    
    private void validateBasicCredential(BasicCredential credential) {
        if (credential.getUsername() == null || credential.getUsername().trim().isEmpty()) {
            log.error("Basic credential username is null or empty");
            throw new RuntimeException("Basic credential username is null or empty");
        }
        if (credential.getPassword() == null || credential.getPassword().trim().isEmpty()) {
            log.error("Basic credential password is null or empty");
            throw new RuntimeException("Basic credential password is null or empty");
        }
    }
    
    private void validateApiKeyCredential(ApiKeyCredential credential) {
        if (credential.getHeaderName() == null || credential.getHeaderName().trim().isEmpty()) {
            log.error("API key credential header name is null or empty");
            throw new RuntimeException("API key credential header name is null or empty");
        }
        if (credential.getApiKey() == null || credential.getApiKey().trim().isEmpty()) {
            log.error("API key credential API key is null or empty");
            throw new RuntimeException("API key credential API key is null or empty");
        }
    }
    
    private void validateOAuthCredential(OAuthCredential credential) {
        if (credential.getAccessToken() == null || credential.getAccessToken().trim().isEmpty()) {
            log.error("OAuth credential access token is null or empty");
            throw new RuntimeException("OAuth credential access token is null or empty");
        }
    }
    
    private void validateAwsCredential(AwsCredential credential) {
        if (credential.getAccessKeyId() == null || credential.getAccessKeyId().trim().isEmpty()) {
            log.error("AWS credential access key ID is null or empty");
            throw new RuntimeException("AWS credential access key ID is null or empty");
        }
        if (credential.getSecretAccessKey() == null || credential.getSecretAccessKey().trim().isEmpty()) {
            log.error("AWS credential secret access key is null or empty");
            throw new RuntimeException("AWS credential secret access key is null or empty");
        }
    }
    
    /**
     * Gets authentication headers for HTTP requests based on credential type
     */
    public Map<String, String> getAuthHeaders(Credential credential) {
        Map<String, String> headers = new HashMap<>();
        
        if (credential == null) {
            log.warn("Cannot get auth headers: credential is null");
            return headers;
        }
        
        try {
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
                    AwsCredential aws = (AwsCredential) credential;
                    headers.put("X-AWS-Access-Key-Id", aws.getAccessKeyId());
                    headers.put("X-AWS-Secret-Access-Key", aws.getSecretAccessKey());
                    break;
                    
                default:
                    log.warn("Unknown credential type for auth headers: {}", credential.getType());
            }
        } catch (Exception e) {
            log.error("Error creating auth headers for credential type: {} - {}", 
                    credential.getType(), e.getMessage(), e);
        }
        
        return headers;
    }
    
    /**
     * Gets AWS credentials for S3/Storj access
     */
    public AwsCredential getAwsCredential(Credential credential) {
        if (credential == null) {
            log.warn("Cannot get AWS credential: credential is null");
            return null;
        }
        
        if (!"aws".equals(credential.getType())) {
            log.warn("Cannot get AWS credential: credential type is {}, expected aws", credential.getType());
            return null;
        }
        
        try {
            return (AwsCredential) credential;
        } catch (ClassCastException e) {
            log.error("Failed to cast credential to AwsCredential - {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Gets MinIO credentials
     */
    public BasicCredential getMinioCredential(Credential credential) {
        if (credential == null) {
            log.warn("Cannot get MinIO credential: credential is null");
            return null;
        }
        
        if (!"basic".equals(credential.getType())) {
            log.warn("Cannot get MinIO credential: credential type is {}, expected basic", credential.getType());
            return null;
        }
        
        try {
            return (BasicCredential) credential;
        } catch (ClassCastException e) {
            log.error("Failed to cast credential to BasicCredential - {}", e.getMessage(), e);
            return null;
        }
    }
} 