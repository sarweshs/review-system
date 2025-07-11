package com.reviewcore.model;

public class BasicCredential extends Credential {
    private String username;
    private String password;
    
    public BasicCredential() {
        super("basic");
    }
    
    public BasicCredential(String username, String password) {
        super("basic");
        this.username = username;
        this.password = password;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
} 