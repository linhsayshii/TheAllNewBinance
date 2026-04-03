package com.auction.core.auction;

import com.auction.core.Entity;

public class Bid extends Entity {
    private Integer id;
    private Integer auctionId;
    private Integer bidderId;
    private Double amount;

    public Bid(Integer id, Integer auctionId, Integer bidderId, Double amount) {
        super();
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getAuctionId() { return auctionId; }
    public Integer getBidderId() { return bidderId; }
    public Double getAmount() { return amount; }
}
