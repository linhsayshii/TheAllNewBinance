package com.auction.core.dto.auction;

public class GetAuctionDetailsRequest {
    private Integer auctionId;

    public GetAuctionDetailsRequest() {}

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
