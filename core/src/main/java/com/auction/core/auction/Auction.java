package com.auction.core.auction;

import java.time.Duration;
import java.time.LocalDateTime;

import com.auction.core.Entity;

public class Auction extends Entity {
    private static final int THRESHOLD_SECONDS = 120;
    private static final int EXTENSION_SECONDS = 120;
    private int id;                   // ID phiên đấu giá
    private int itemId;               // ID của sản phẩm (Item)
    private double startingPrice;     // Giá khởi điểm
    private double currentPrice;      // Giá hiện tại
    private double bidIncrement;      // Bước giá
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime originalEndTime;
    public enum Status { PENDING, ACTIVE, ENDED, CANCELLED }
    private Status status;
    private int winnerId;
    private double finalPrice;

    
    public Auction() {
        super();
    }

    public Auction(Integer id, Integer itemId, Double startingPrice, Double bidIncrement, LocalDateTime startTime, LocalDateTime originalEndTime) {
        super();
        this.id = id;
        this.itemId = itemId;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.bidIncrement = bidIncrement != null ? bidIncrement : 1.0;
        this.startTime = startTime;
        this.originalEndTime = originalEndTime;
        this.endTime = originalEndTime;
        this.status = Status.PENDING;
    }

    public boolean applySnipeExtension(LocalDateTime bidTime) {
        if (bidTime == null || endTime == null) {
            return false;
        }
        if (!bidTime.isBefore(endTime)) {
            return false;
        }
        long secondsRemaining = Duration.between(bidTime, endTime).getSeconds();
        if (secondsRemaining <= THRESHOLD_SECONDS) {
            LocalDateTime newEndTime = bidTime.plusSeconds(EXTENSION_SECONDS);
            if (newEndTime.isAfter(endTime)) {
                endTime = newEndTime;
                updateTimestamp();
                return true;
            }
        }
        return false;
    }

    public Integer[] getSnipeSettings() {
        return new Integer[] { THRESHOLD_SECONDS, EXTENSION_SECONDS };
    }

    public Integer getSnipeThreshold() { return THRESHOLD_SECONDS; }
    public Integer getSnipeExtension() { return EXTENSION_SECONDS; }
    
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }
    
    public Double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(Double startingPrice) { this.startingPrice = startingPrice; this.updateTimestamp(); }
    
    public Double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(Double currentPrice) { this.currentPrice = currentPrice; this.updateTimestamp(); }
    
    public Double getBidIncrement() { return bidIncrement; }
    public void setBidIncrement(Double bidIncrement) { this.bidIncrement = bidIncrement; this.updateTimestamp(); }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; this.updateTimestamp(); }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; this.updateTimestamp(); }

    public LocalDateTime getOriginalEndTime() { return originalEndTime; }
    public void setOriginalEndTime(LocalDateTime originalEndTime) { this.originalEndTime = originalEndTime; this.updateTimestamp(); }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; this.updateTimestamp(); }
    
    public Integer getWinnerId() { return winnerId; }
    public void setWinnerId(Integer winnerId) { this.winnerId = winnerId; this.updateTimestamp(); }

    public Double getFinalPrice() { return finalPrice; }
    public void setFinalPrice(Double finalPrice) { this.finalPrice = finalPrice; this.updateTimestamp(); }
}