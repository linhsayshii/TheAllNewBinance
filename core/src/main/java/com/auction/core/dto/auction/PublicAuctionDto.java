package com.auction.core.dto.auction;

import java.time.LocalDateTime;

public class PublicAuctionDto {
    private Integer auctionId;
    private Integer itemId;
    private String itemName;
    private String thumbnailUrl;
    private Double currentPrice;
    private LocalDateTime endTime;
    private String sellerDisplayName;
    private String status;

    public PublicAuctionDto() {}

    public PublicAuctionDto(Integer auctionId, Integer itemId, String itemName, String thumbnailUrl, Double currentPrice, LocalDateTime endTime, String sellerDisplayName, String status) {
        this.auctionId = auctionId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.thumbnailUrl = thumbnailUrl;
        this.currentPrice = currentPrice;
        this.endTime = endTime;
        this.sellerDisplayName = sellerDisplayName;
        this.status = status;
    }

    public Integer getAuctionId() {return auctionId; }
    public void setAuctionId(Integer auctionId) {this.auctionId = auctionId;}

    public Integer getItemId() {return itemId; }
    public void setItemId(Integer itemId) {this.itemId = itemId; }

    public String getItemName() {return itemName; }
    public void setItemName(String itemName) {this.itemName = itemName;}

    public String getThumbnailUrl() {return thumbnailUrl;}
    public void setThumbnailUrl(String thumbnailUrl) {this.thumbnailUrl = thumbnailUrl; }

    public Double getCurrentPrice() {return currentPrice;}
    public void setCurrentPrice(Double currentPrice) {this.currentPrice = currentPrice;}

    public LocalDateTime getEndTime() {return endTime; }
    public void setEndTime(LocalDateTime endTime) {this.endTime = endTime;}

    public String getSellerDisplayName() {return sellerDisplayName; }
    public void setSellerDisplayName(String sellerDisplayName) {this.sellerDisplayName = sellerDisplayName; }

    public String getStatus() {return status;}
    public void setStatus(String status) {this.status = status;}
}
