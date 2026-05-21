package com.auction.server.dao.impl;

import com.auction.core.auction.Bid;
import java.util.List;

public interface IBidDao {
    boolean saveBid(Bid bid);

    List<Bid> findByAuctionId(Integer auctionId);

    List<Bid> findByBidderId(Integer bidderId);

    boolean hasBid(Integer auctionId, Integer bidderId);
}
