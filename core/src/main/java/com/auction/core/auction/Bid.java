package com.auction.core.auction;

import com.auction.core.Entity;

public class Bid extends Entity {
    private Integer id;
    private Integer auctionId;
    private Integer bidderId;
    private Double amount;
    private Boolean isAuto;

    public Bid(Integer id, Integer auctionId, Integer bidderId, Double amount, Boolean isAuto) {
        super();
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.isAuto = isAuto != null ? isAuto : false;
    }

    // Constructor dùng khi User vừa đặt Bid mới
    public Bid(Integer auctionId, Integer bidderId, Double amount, Boolean isAuto) {
        this(null, auctionId, bidderId, amount, isAuto);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getAuctionId() { return auctionId; }
    public Integer getBidderId() { return bidderId; }
    public Double getAmount() { return amount; }
    public Boolean getIsAuto() { return isAuto; }
}
