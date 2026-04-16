package com.auction.core.dao;

import java.util.List;

import com.auction.core.auction.Bid;

public interface IBidDao {
    boolean saveBid(Bid bid);
    List<Bid> findByAuctionId(Integer auctionId);
    List<Bid> findByBidderId(Integer bidderId);
    boolean hasBid(Integer auctionId, Integer bidderId);
}
