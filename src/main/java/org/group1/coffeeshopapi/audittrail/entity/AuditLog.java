package org.group1.coffeeshopapi.audittrail.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true, length = 50)
	private String auditId;

	@Column(nullable = false, length = 100)
	private String actorId;

	@Column(nullable = false, length = 150)
	private String actorName;

	@Column(nullable = false, length = 50)
	private String role;

	@Column(nullable = false, length = 80)
	private String entity;

	@Column(length = 100)
	private String entityId;

	@Column(nullable = false, length = 50)
	private String action;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	@Column(length = 64)
	private String ipAddress;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
		if (auditId == null || auditId.isBlank()) {
			auditId = "AUD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		}
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}