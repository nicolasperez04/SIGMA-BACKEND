package com.SIGMA.USCO.config;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;

@MappedSuperclass
public class BaseEntity {

    @Column(name = "created_at", nullable = false)
    protected LocalDateTime createdAt;

    @Column(name = "updated_at")
    protected LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = DateUtilsConfig.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = DateUtilsConfig.now();
    }



}
