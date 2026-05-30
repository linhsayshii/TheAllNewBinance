package com.auction.core.dto.auction;

import java.time.LocalDateTime;

public class CreateAuctionRequest {
    private Integer sellerId;

    // Item details
    private String itemTitle;
    private String itemDescription;
    private String itemCategory;
    private String itemImageUrl;

    // Auction details
    private Double startingPrice;
    private Double bidIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /**
     * Polymorphic payload containing product-group-specific attributes. Serialized over the network
     * with a {@code "type"} discriminator by {@code ItemAttributesPayloadSerializer}.
     */
    private ItemAttributesPayload attributes;

    public CreateAuctionRequest() {}

    public CreateAuctionRequest(
            Integer sellerId,
            String itemTitle,
            String itemDescription,
            String itemCategory,
            String itemImageUrl,
            Double startingPrice,
            Double bidIncrement,
            LocalDateTime startTime,
            LocalDateTime endTime,
            ItemAttributesPayload attributes) {
        this.sellerId = sellerId;
        this.itemTitle = itemTitle;
        this.itemDescription = itemDescription;
        this.itemCategory = itemCategory;
        this.itemImageUrl = itemImageUrl;
        this.startingPrice = startingPrice;
        this.bidIncrement = bidIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
        this.attributes = attributes;
    }

    public Integer getSellerId() {
        return sellerId;
    }

    public void setSellerId(Integer sellerId) {
        this.sellerId = sellerId;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public void setItemTitle(String itemTitle) {
        this.itemTitle = itemTitle;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public String getItemCategory() {
        return itemCategory;
    }

    public void setItemCategory(String itemCategory) {
        this.itemCategory = itemCategory;
    }

    public String getItemImageUrl() {
        return itemImageUrl;
    }

    public void setItemImageUrl(String itemImageUrl) {
        this.itemImageUrl = itemImageUrl;
    }

    public Double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(Double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public Double getBidIncrement() {
        return bidIncrement;
    }

    public void setBidIncrement(Double bidIncrement) {
        this.bidIncrement = bidIncrement;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public ItemAttributesPayload getAttributes() {
        return attributes;
    }

    public void setAttributes(ItemAttributesPayload attributes) {
        this.attributes = attributes;
    }
}
