package com.chat.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base class for auditing entity data, providing common fields such as
 * createdDate, updatedDate. Uses JPA and Spring Data
 * JPA annotations for automatic management of these fields.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Auditable {
	/**
	 * The date and time when the entity was created. Managed automatically by
	 * Spring Data JPA.
	 */
	@CreatedDate
	@Column(nullable = false, columnDefinition = "DATETIME(0)")
	private LocalDateTime createdDate;

	/**
	 * The date and time when the entity was last updated. Managed automatically by
	 * Spring Data JPA.
	 */
	@LastModifiedDate
	@Column(nullable = false, columnDefinition = "DATETIME(0)")
	private LocalDateTime updatedDate;
}
