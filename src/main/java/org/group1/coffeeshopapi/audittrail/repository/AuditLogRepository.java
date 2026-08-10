package org.group1.coffeeshopapi.audittrail.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.group1.coffeeshopapi.audittrail.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE (LOWER(a.actorId) LIKE LOWER(CONCAT('%', :actor, '%')) OR LOWER(a.actorName) LIKE LOWER(CONCAT('%', :actor, '%'))) ORDER BY a.createdAt DESC")
    Page<AuditLog> findByActorNameOrActorId(@Param("actor") String actor, Pageable pageable);

    Page<AuditLog> findByEntityContainingIgnoreCaseOrderByCreatedAtDesc(String entity, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE (:actorId IS NULL OR LOWER(a.actorId) LIKE LOWER(CONCAT('%', :actorId, '%')) OR LOWER(a.actorName) LIKE LOWER(CONCAT('%', :actorId, '%'))) AND (:entity IS NULL OR LOWER(a.entity) LIKE LOWER(CONCAT('%', :entity, '%'))) AND (:from IS NULL OR a.createdAt >= :from) AND (:to IS NULL OR a.createdAt <= :to) ORDER BY a.createdAt DESC")
    Page<AuditLog> queryWithFilters(@Param("actorId") String actorId, @Param("entity") String entity,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
}
