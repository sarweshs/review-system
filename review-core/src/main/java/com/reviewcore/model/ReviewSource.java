package com.reviewcore.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_sources")
public class ReviewSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String uri;
    private LocalDateTime lastProcessedTimestamp;
    
    @Column(columnDefinition = "TEXT")
    private String credentialJson;

    private Boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
    public LocalDateTime getLastProcessedTimestamp() { return lastProcessedTimestamp; }
    public void setLastProcessedTimestamp(LocalDateTime lastProcessedTimestamp) { this.lastProcessedTimestamp = lastProcessedTimestamp; }
    
    public String getCredentialJson() { return credentialJson; }
    public void setCredentialJson(String credentialJson) { this.credentialJson = credentialJson; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
} 