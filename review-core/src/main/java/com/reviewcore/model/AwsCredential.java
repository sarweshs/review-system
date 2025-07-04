package com.reviewcore.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AwsCredential extends Credential {
    
    @JsonProperty("accessKeyId")
    private String accessKeyId;
    
    @JsonProperty("secretAccessKey")
    private String secretAccessKey;
    
    public AwsCredential() {
        super("aws");
        // Default constructor for Jackson
    }
    
    public AwsCredential(String accessKeyId, String secretAccessKey) {
        super("aws");
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
    }
    
    public String getAccessKeyId() {
        return accessKeyId;
    }
    
    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }
    
    public String getSecretAccessKey() {
        return secretAccessKey;
    }
    
    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }
} 