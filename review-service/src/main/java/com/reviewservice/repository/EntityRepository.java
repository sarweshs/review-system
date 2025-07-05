package com.reviewservice.repository;

import com.reviewcore.model.ReviewEntity;
import com.reviewcore.model.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntityRepository extends JpaRepository<ReviewEntity, Integer> {
    
    /**
     * Find entity by name and type
     */
    Optional<ReviewEntity> findByEntityNameAndEntityType(String entityName, EntityType entityType);
    
    /**
     * Check if entity exists by name and type
     */
    boolean existsByEntityNameAndEntityType(String entityName, EntityType entityType);
    
    /**
     * Find entity by name (case-insensitive)
     */
    @Query("SELECT e FROM ReviewEntity e WHERE LOWER(e.entityName) = LOWER(:entityName)")
    Optional<ReviewEntity> findByEntityNameIgnoreCase(@Param("entityName") String entityName);
} 