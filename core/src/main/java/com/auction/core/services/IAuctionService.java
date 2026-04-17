package com.auction.core.services;

import java.util.List;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.dto.AuctionService.CreateAuctionRequest;
import com.auction.core.dto.AuctionService.GetAuctionBySellerIdRequest;

public interface IAuctionService {
    void processBid(Bid bid, Auction auction);
    Auction createAuction(CreateAuctionRequest request);
    Auction deleteAuction(Integer auctionId);
    boolean validateBid(Integer auctionId, Double amount);
    boolean applySnipeExtension(Auction auction);
    Auction getAuctionDetails(Integer auctionId);
    Integer getSellerId(Integer auctionId);
    List<Auction> getAuctionsBySellerId(GetAuctionBySellerIdRequest request);
}