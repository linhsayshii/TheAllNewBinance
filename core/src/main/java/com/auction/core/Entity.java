package com.auction.core;

import java.time.LocalDateTime;

import com.auction.core.utils.DateFormatter;

public abstract class Entity {
    protected LocalDateTime createdAt; // thời gian tạo 
    protected LocalDateTime updatedAt; // thời gian update mới nhất
    protected boolean isDeleted;       // bị xóa hay chưa

    public Entity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isDeleted = false;
    }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    protected void updateTimestamp() {this.updatedAt = LocalDateTime.now(); }

    public boolean isDeleted() {return isDeleted;}
    public void setDeleted(boolean deleted) { this.isDeleted = deleted; updateTimestamp(); }
    
    public String getFormattedCreatedAt() {return DateFormatter.format(createdAt);}
    public String getFormattedUpdatedAt() {return DateFormatter.format(updatedAt);
    }
}