package com.reviewcore.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Entity model representing a reviewable entity (hotel, airline, etc.)
 */
@Entity
@Table(name = "entities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entity_id")
    private Integer entityId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private EntityType entityType;
    
    @Column(name = "entity_name", nullable = false)
    private String entityName;
} 