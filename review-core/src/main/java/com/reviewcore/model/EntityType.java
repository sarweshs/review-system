package com.reviewcore.model;

/**
 * Enum representing different types of entities that can be reviewed
 */
public enum EntityType {
    HOTEL,
    AIRLINE,
    HOSTEL,
    RESTAURANT,
    ATTRACTION,
    UNKNOWN;

    /**
     * Extract entity type from an ID string
     * @param id The ID to analyze
     * @return The extracted entity type
     */
    public static EntityType fromId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return UNKNOWN;
        }
        
        String lowerId = id.toLowerCase();
        
        if (lowerId.contains("hotel") || lowerId.startsWith("hotel")) {
            return HOTEL;
        } else if (lowerId.contains("airline") || lowerId.startsWith("airline")) {
            return AIRLINE;
        } else if (lowerId.contains("hostel") || lowerId.startsWith("hostel")) {
            return HOSTEL;
        } else if (lowerId.contains("restaurant") || lowerId.startsWith("restaurant")) {
            return RESTAURANT;
        } else if (lowerId.contains("attraction") || lowerId.startsWith("attraction")) {
            return ATTRACTION;
        }
        
        return UNKNOWN;
    }
} 