package com.auction.core;

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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; this.updateTimestamp(); }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; this.updateTimestamp();}   

    protected void updateTimestamp() {this.updatedAt = LocalDateTime.now(); }

    public boolean isDeleted() {return isDeleted;}
    public void setDeleted(boolean deleted) { this.isDeleted = deleted; updateTimestamp(); }
}