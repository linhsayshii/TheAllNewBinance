package com.auction.core.dto.AuctionService;

public class GetAuctionDetailsRequest {
    private Integer auctionId;

    public GetAuctionDetailsRequest() {
    }

    public GetAuctionDetailsRequest(Integer auctionId) {
        this.auctionId = auctionId;
    }

    public Integer getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Integer auctionId) {
        this.auctionId = auctionId;
    }
}
