package com.auction.core.dto.auction;

import java.time.LocalDateTime;

public class CreateAuctionRequest {
    private Integer sellerId;
    private Integer itemId;
    private Double startingPrice;
    private Double bidIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public CreateAuctionRequest() {}

    public CreateAuctionRequest(Integer sellerId, Integer itemId, Double startingPrice, Double bidIncrement, LocalDateTime startTime, LocalDateTime endTime) {
        this.sellerId = sellerId;
        this.itemId = itemId;
        this.startingPrice = startingPrice;
        this.bidIncrement = bidIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Integer getSellerId() {return sellerId; }
    public void setSellerId(Integer sellerId) {this.sellerId = sellerId; }

    public Integer getItemId() {return itemId; }
    public void setItemId(Integer itemId) {this.itemId = itemId; }

    public Double getStartingPrice() {return startingPrice; }
    public void setStartingPrice(Double startingPrice) {this.startingPrice = startingPrice;}

    public Double getBidIncrement() {return bidIncrement; }
    public void setBidIncrement(Double bidIncrement) {this.bidIncrement = bidIncrement;}

    public LocalDateTime getStartTime() {return startTime; }
    public void setStartTime(LocalDateTime startTime) {this.startTime = startTime; }

    public LocalDateTime getEndTime() {return endTime; }
    public void setEndTime(LocalDateTime endTime) {this.endTime = endTime; }
}
