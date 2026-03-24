package com.auction.core.services;

import java.time.LocalDateTime;

import com.auction.core.auction.Auction;
import com.auction.core.products.Item;

public interface IAuctionService {
    void placeBid(Integer userId, Integer auctionId, Double amount);
    Auction createAuction(Item item, Double startingPrice, Double bidIncrement, LocalDateTime startTime, LocalDateTime endTime);
}