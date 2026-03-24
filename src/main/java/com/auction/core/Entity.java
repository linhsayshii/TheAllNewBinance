package com.auction.core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
public abstract class Entity {
    protected LocalDateTime createdAt; // thời gian tạo 
    protected LocalDateTime updatedAt; // thời gian update mới nhất
    protected boolean isDeleted;       // bị xóa hay chưa

    public Entity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isDeleted = false;
    }

    public void markAsDeleted() {
        this.isDeleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    protected void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public String getFormattedCreatedAt() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return createdAt.format(formatter);
    }
}