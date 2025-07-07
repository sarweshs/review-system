package com.reviewproducer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

/**
 * Generic DTO for storage events from different providers (MinIO, S3, Azure Blob, etc.)
 */
public class StorageEvent {
    
    @JsonProperty("provider")
    private String provider; // "minio", "s3", "azure", "gcs"
    
    @JsonProperty("eventType")
    private String eventType; // "ObjectCreated", "ObjectDeleted", etc.
    
    @JsonProperty("bucket")
    private String bucket;
    
    @JsonProperty("key")
    private String key;
    
    @JsonProperty("size")
    private Long size;
    
    @JsonProperty("etag")
    private String etag;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("metadata")
    private Map<String, String> metadata;
    
    @JsonProperty("rawEvent")
    private String rawEvent; // Original event payload as JSON string
    
    public StorageEvent() {}
    
    public StorageEvent(String provider, String eventType, String bucket, String key, 
                       Long size, String etag, Instant timestamp, Map<String, String> metadata, String rawEvent) {
        this.provider = provider;
        this.eventType = eventType;
        this.bucket = bucket;
        this.key = key;
        this.size = size;
        this.etag = etag;
        this.timestamp = timestamp;
        this.metadata = metadata;
        this.rawEvent = rawEvent;
    }
    
    // Getters and setters
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
    
    public String getEtag() { return etag; }
    public void setEtag(String etag) { this.etag = etag; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    
    public String getRawEvent() { return rawEvent; }
    public void setRawEvent(String rawEvent) { this.rawEvent = rawEvent; }
    
    /**
     * Check if this event is for a .jl file
     */
    public boolean isJsonlFile() {
        return key != null && key.endsWith(".jl");
    }
    
    /**
     * Check if this is a file creation event
     */
    public boolean isFileCreated() {
        return "ObjectCreated".equals(eventType) || 
               "ObjectCreated:Put".equals(eventType) ||
               "ObjectCreated:Post".equals(eventType);
    }
    
    /**
     * Get the file name from the key
     */
    public String getFileName() {
        if (key == null) return null;
        int lastSlash = key.lastIndexOf('/');
        return lastSlash >= 0 ? key.substring(lastSlash + 1) : key;
    }
    
    @Override
    public String toString() {
        return "StorageEvent{" +
                "provider='" + provider + '\'' +
                ", eventType='" + eventType + '\'' +
                ", bucket='" + bucket + '\'' +
                ", key='" + key + '\'' +
                ", size=" + size +
                ", etag='" + etag + '\'' +
                ", timestamp=" + timestamp +
                ", fileName='" + getFileName() + '\'' +
                ", isJsonlFile=" + isJsonlFile() +
                ", isFileCreated=" + isFileCreated() +
                '}';
    }
} 