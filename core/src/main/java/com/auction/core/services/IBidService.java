package com.auction.core.services;

import java.util.List;

import com.auction.core.auction.Bid;
import com.auction.core.users.User;

public interface IBidService {
    Bid placeBid(Integer auctionId, Integer bidderId, Double amount);
    boolean validateUserBid(Integer auctionId, User user);
    void automaticallyPlaceBid(Integer auctionId, double increment, double maxAmount);
    List<Bid> getBidsByAuctionId(Integer auctionId);
    List<Bid> getBidsByBidderId(Integer bidderId);
}
