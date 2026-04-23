package com.auction.server.dao.impl;

import java.util.List;

import com.auction.core.auction.Bid;

public interface IBidDao {
    boolean saveBid(Bid bid);
    List<Bid> findByAuctionId(Integer auctionId);
    List<Bid> findByBidderId(Integer bidderId);
    boolean hasBid(Integer auctionId, Integer bidderId);
}
