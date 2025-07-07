package com.reviewproducer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO for MinIO events containing information about uploaded files
 */
public class MinIOEvent {
    
    @JsonProperty("EventName")
    private String eventName;
    
    @JsonProperty("Key")
    private String key;
    
    @JsonProperty("Records")
    private List<MinIORecord> records;
    
    public MinIOEvent() {}
    
    public MinIOEvent(String eventName, String key, List<MinIORecord> records) {
        this.eventName = eventName;
        this.key = key;
        this.records = records;
    }
    
    // Getters and setters
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    
    public List<MinIORecord> getRecords() { return records; }
    public void setRecords(List<MinIORecord> records) { this.records = records; }
    
    /**
     * Inner class representing individual MinIO records
     */
    public static class MinIORecord {
        @JsonProperty("eventVersion")
        private String eventVersion;
        
        @JsonProperty("eventSource")
        private String eventSource;
        
        @JsonProperty("awsRegion")
        private String awsRegion;
        
        @JsonProperty("eventTime")
        private String eventTime;
        
        @JsonProperty("eventName")
        private String eventName;
        
        @JsonProperty("s3")
        private S3Info s3;
        
        public MinIORecord() {}
        
        public MinIORecord(String eventVersion, String eventSource, String awsRegion, 
                          String eventTime, String eventName, S3Info s3) {
            this.eventVersion = eventVersion;
            this.eventSource = eventSource;
            this.awsRegion = awsRegion;
            this.eventTime = eventTime;
            this.eventName = eventName;
            this.s3 = s3;
        }
        
        // Getters and setters
        public String getEventVersion() { return eventVersion; }
        public void setEventVersion(String eventVersion) { this.eventVersion = eventVersion; }
        
        public String getEventSource() { return eventSource; }
        public void setEventSource(String eventSource) { this.eventSource = eventSource; }
        
        public String getAwsRegion() { return awsRegion; }
        public void setAwsRegion(String awsRegion) { this.awsRegion = awsRegion; }
        
        public String getEventTime() { return eventTime; }
        public void setEventTime(String eventTime) { this.eventTime = eventTime; }
        
        public String getEventName() { return eventName; }
        public void setEventName(String eventName) { this.eventName = eventName; }
        
        public S3Info getS3() { return s3; }
        public void setS3(S3Info s3) { this.s3 = s3; }
    }
    
    /**
     * Inner class representing S3/MinIO object information
     */
    public static class S3Info {
        @JsonProperty("s3SchemaVersion")
        private String s3SchemaVersion;
        
        @JsonProperty("configurationId")
        private String configurationId;
        
        @JsonProperty("bucket")
        private BucketInfo bucket;
        
        @JsonProperty("object")
        private ObjectInfo object;
        
        public S3Info() {}
        
        public S3Info(String s3SchemaVersion, String configurationId, BucketInfo bucket, ObjectInfo object) {
            this.s3SchemaVersion = s3SchemaVersion;
            this.configurationId = configurationId;
            this.bucket = bucket;
            this.object = object;
        }
        
        // Getters and setters
        public String getS3SchemaVersion() { return s3SchemaVersion; }
        public void setS3SchemaVersion(String s3SchemaVersion) { this.s3SchemaVersion = s3SchemaVersion; }
        
        public String getConfigurationId() { return configurationId; }
        public void setConfigurationId(String configurationId) { this.configurationId = configurationId; }
        
        public BucketInfo getBucket() { return bucket; }
        public void setBucket(BucketInfo bucket) { this.bucket = bucket; }
        
        public ObjectInfo getObject() { return object; }
        public void setObject(ObjectInfo object) { this.object = object; }
    }
    
    /**
     * Inner class representing bucket information
     */
    public static class BucketInfo {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("ownerIdentity")
        private OwnerIdentity ownerIdentity;
        
        @JsonProperty("arn")
        private String arn;
        
        public BucketInfo() {}
        
        public BucketInfo(String name, OwnerIdentity ownerIdentity, String arn) {
            this.name = name;
            this.ownerIdentity = ownerIdentity;
            this.arn = arn;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public OwnerIdentity getOwnerIdentity() { return ownerIdentity; }
        public void setOwnerIdentity(OwnerIdentity ownerIdentity) { this.ownerIdentity = ownerIdentity; }
        
        public String getArn() { return arn; }
        public void setArn(String arn) { this.arn = arn; }
    }
    
    /**
     * Inner class representing object information
     */
    public static class ObjectInfo {
        @JsonProperty("key")
        private String key;
        
        @JsonProperty("size")
        private Long size;
        
        @JsonProperty("eTag")
        private String eTag;
        
        @JsonProperty("sequencer")
        private String sequencer;
        
        public ObjectInfo() {}
        
        public ObjectInfo(String key, Long size, String eTag, String sequencer) {
            this.key = key;
            this.size = size;
            this.eTag = eTag;
            this.sequencer = sequencer;
        }
        
        // Getters and setters
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        
        public Long getSize() { return size; }
        public void setSize(Long size) { this.size = size; }
        
        public String getETag() { return eTag; }
        public void setETag(String eTag) { this.eTag = eTag; }
        
        public String getSequencer() { return sequencer; }
        public void setSequencer(String sequencer) { this.sequencer = sequencer; }
    }
    
    /**
     * Inner class representing owner identity
     */
    public static class OwnerIdentity {
        @JsonProperty("principalId")
        private String principalId;
        
        public OwnerIdentity() {}
        
        public OwnerIdentity(String principalId) {
            this.principalId = principalId;
        }
        
        // Getters and setters
        public String getPrincipalId() { return principalId; }
        public void setPrincipalId(String principalId) { this.principalId = principalId; }
    }
} 