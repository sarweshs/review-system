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
            return null;
        }
        
        String decryptedJson = vaultService.decrypt(encryptedJson);
        return objectMapper.readValue(decryptedJson, Credential.class);
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
                AwsCredential aws = (AwsCredential) credential;
                headers.put("X-AWS-Access-Key-Id", aws.getAccessKeyId());
                headers.put("X-AWS-Secret-Access-Key", aws.getSecretAccessKey());
                break;
        }
        
        return headers;
    }
    
    /**
     * Gets AWS credentials for S3/Storj access
     */
    public AwsCredential getAwsCredential(Credential credential) {
        if (credential != null && "aws".equals(credential.getType())) {
            return (AwsCredential) credential;
        }
        return null;
    }
    
    /**
     * Gets MinIO credentials
     */
    public BasicCredential getMinioCredential(Credential credential) {
        if (credential != null && "basic".equals(credential.getType())) {
            return (BasicCredential) credential;
        }
        return null;
    }
} 