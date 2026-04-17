package com.auction.core.dto.AuctionService;

public class GetAuctionBySellerIdRequest {
    private Integer sellerId;

    public GetAuctionBySellerIdRequest() {
    }

    public GetAuctionBySellerIdRequest(Integer sellerId) {
        this.sellerId = sellerId;
    }

    public Integer getSellerId() {
        return sellerId;
    }

    public void setSellerId(Integer sellerId) {
        this.sellerId = sellerId;
    }
}
