package com.problemfighter.java.base.entity;

import com.palantir.humanreadabletypes.HumanReadableByteCount;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class BaseUserEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @CreatedDate
    @Column(name = "create_date", nullable = false, updatable = false)
    private OffsetDateTime createDate;

    @LastModifiedDate
    @Column(name = "update_date", nullable = false)
    private OffsetDateTime updateDate;

    @CreatedBy
    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "uuid", unique = true)
    private String uuid;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    private HumanReadableByteCount size;


    @PrePersist
    private void onBasePersist() {
        if (this.uuid == null || this.uuid.isEmpty())
            this.uuid = UUID.randomUUID().toString();
    }

    @PreUpdate
    private void onBaseUpdate() {
        if (this.uuid == null || this.uuid.isEmpty())
            this.uuid = UUID.randomUUID().toString();
    }

}

