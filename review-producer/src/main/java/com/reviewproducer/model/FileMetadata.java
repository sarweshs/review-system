package com.reviewproducer.model;

import java.time.Instant;

/**
 * Metadata for files stored in cloud storage
 */
public class FileMetadata {
    private final String name;
    private final String key;
    private final long size;
    private final Instant lastModified;
    private final Instant created;
    private final String etag;
    private final String contentType;

    public FileMetadata(String name, String key, long size, Instant lastModified, Instant created, String etag, String contentType) {
        this.name = name;
        this.key = key;
        this.size = size;
        this.lastModified = lastModified;
        this.created = created;
        this.etag = etag;
        this.contentType = contentType;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public long getSize() {
        return size;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public Instant getCreated() {
        return created;
    }

    public String getEtag() {
        return etag;
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public String toString() {
        return "FileMetadata{" +
                "name='" + name + '\'' +
                ", key='" + key + '\'' +
                ", size=" + size +
                ", lastModified=" + lastModified +
                ", created=" + created +
                ", etag='" + etag + '\'' +
                ", contentType='" + contentType + '\'' +
                '}';
    }
} 