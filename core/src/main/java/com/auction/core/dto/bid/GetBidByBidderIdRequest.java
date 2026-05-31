package com.auction.core.dto.bid;

import com.auction.core.auction.Bid;
import java.util.ArrayList;
import java.util.List;

public class GetBidByBidderIdRequest {
    private Integer bidderId;
    private Integer auctionID;
    private List<Bid> bids = new ArrayList<>();

    public GetBidByBidderIdRequest() {}

    public GetBidByBidderIdRequest(Integer bidderId, Integer auctionID, List<Bid> bids) {
        this.bidderId = bidderId;
        this.auctionID = auctionID;
        this.bids = bids;
    }

    public Integer getBidderId() {
        return bidderId;
    }

    public void setBidderId(Integer bidderId) {
        this.bidderId = bidderId;
    }

    public Integer getAuctionID() {
        return auctionID;
    }

    public void setAuctionID(Integer auctionID) {
        this.auctionID = auctionID;
    }

    public List<Bid> getBids() {
        return bids;
    }

    public void setBids(List<Bid> bids) {
        this.bids = bids;
    }
}
