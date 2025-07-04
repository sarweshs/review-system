package com.reviewcore.model;

public class ApiKeyCredential extends Credential {
    private String apiKey;
    private String headerName;
    
    public ApiKeyCredential() {
        super("apikey");
    }
    
    public ApiKeyCredential(String apiKey, String headerName) {
        super("apikey");
        this.apiKey = apiKey;
        this.headerName = headerName;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getHeaderName() {
        return headerName;
    }
    
    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }
} 