package com.auction.core.dto.bid;

import java.util.ArrayList;
import java.util.List;

import com.auction.core.auction.Bid;

public class GetBidByAuctionIdRequest {
    private Integer auctionId;
    private Integer userId;
    private List<Bid> bids = new ArrayList<>();

    public GetBidByAuctionIdRequest() {
    }

    public GetBidByAuctionIdRequest(Integer auctionId, Integer userId, List<Bid> bids) {
        this.auctionId = auctionId;
        this.userId = userId;
        this.bids = bids;
    }

    public Integer getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Integer auctionId) {
        this.auctionId = auctionId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public List<Bid> getBids() {
        return bids;
    }

    public void setBids(List<Bid> bids) {
        this.bids = bids;
    }
}
