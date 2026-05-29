package com.auction.client.dto;

import java.time.LocalDateTime;

public record ProductCardUiModel(
        Integer auctionId,
        String status,
        String title,
        String seller,
        String currentBid,
        String timeLeft,
        LocalDateTime sortTime,
        String imageUrl,
        String category) {

    public ProductCardUiModel(String title, String seller, String currentBid, String timeLeft) {
        this(null, null, title, seller, currentBid, timeLeft, null, null, null);
    }

    public ProductCardUiModel(
            Integer auctionId,
            String status,
            String title,
            String seller,
            String currentBid,
            String timeLeft,
            LocalDateTime sortTime,
            String imageUrl) {
        this(auctionId, status, title, seller, currentBid, timeLeft, sortTime, imageUrl, null);
    }

    public boolean isLive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    public boolean isUpcoming() {
        return "PENDING".equalsIgnoreCase(status);
    }

    public boolean isEnded() {
        return "ENDED".equalsIgnoreCase(status);
    }
}
