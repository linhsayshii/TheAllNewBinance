package com.auction.core;

import com.auction.core.utils.DateFormatter;
import java.time.LocalDateTime;

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
        this.updateTimestamp();
    }

    protected void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public String getFormattedCreatedAt() {
        return DateFormatter.format(createdAt);
    }

    public String getFormattedUpdatedAt() {
        return DateFormatter.format(updatedAt);
    }
}