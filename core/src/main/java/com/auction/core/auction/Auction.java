package com.auction.core.auction;

import java.time.LocalDateTime;

import com.auction.core.Entity;

public class Auction extends Entity {
    private Integer id;               // ID phiên đấu giá
    private Integer itemId;           // ID của sản phẩm (Item)
    private Double startingPrice;     // Giá khởi điểm
    private Double currentPrice;      // Giá hiện tại
    private Double bidIncrement;      // Bước giá
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime originalEndTime;
    public enum Status { PENDING, ACTIVE, ENDED, CANCELLED }
    private Status status;
    private Integer winnerId;
    private Double finalPrice;
    //private Integer snipeThreshold;
    //private Integer snipeExtension;

    public Auction(Integer id, Integer itemId, Double startingPrice, Double bidIncrement, LocalDateTime startTime, LocalDateTime endTime) {
        super();
        this.id = id;
        this.itemId = itemId;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.bidIncrement = bidIncrement != null ? bidIncrement : 1.0;
        this.startTime = startTime;
        this.endTime = endTime;
        this.originalEndTime = endTime;
        this.status = Status.PENDING;
        //this.snipeThreshold = 3;
        //this.snipeExtension = 2;
        
    }

    // Kiểm tra tính hợp lệ của Bid ngay tại Entity (Bussiness Logic thuần)
    public boolean isValidBid(Double newBidAmount) {
        return newBidAmount >= (this.currentPrice + this.bidIncrement) 
            && this.status == Status.ACTIVE
            && LocalDateTime.now().isBefore(this.endTime);
    }
    
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

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; this.updateTimestamp(); }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; this.updateTimestamp(); }
    
    public Integer getWinnerId() { return winnerId; }
    public void setWinnerId(Integer winnerId) { this.winnerId = winnerId; this.updateTimestamp(); }

    public Double getFinalPrice() { return finalPrice; }
    public void setFinalPrice(Double finalPrice) { this.finalPrice = finalPrice; this.updateTimestamp(); }
}