package com.auction.core.services;

import java.time.LocalDateTime;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.products.Item;

public interface IAuctionService {
    void processBid(Bid bid);
    Auction createAuction(Item item, Double startingPrice, Double bidIncrement, LocalDateTime startTime, LocalDateTime endTime);
    Auction deleteAuction(Integer auctionId);
    boolean validateBid(Integer auctionId, Double amount);
    boolean applySnipeExtension(Auction auction);
    Auction getAuctionDetails(Integer auctionId);
}