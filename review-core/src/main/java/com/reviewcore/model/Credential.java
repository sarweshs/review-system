package com.reviewcore.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BasicCredential.class, name = "basic"),
    @JsonSubTypes.Type(value = ApiKeyCredential.class, name = "apikey"),
    @JsonSubTypes.Type(value = OAuthCredential.class, name = "oauth")
})
public abstract class Credential {
    private String type;
    
    public Credential() {}
    
    public Credential(String type) {
        this.type = type;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
} 