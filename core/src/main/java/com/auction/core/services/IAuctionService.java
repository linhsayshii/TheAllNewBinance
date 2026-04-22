package com.auction.core.services;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.dto.auction.CreateAuctionRequest;
import com.auction.core.dto.auction.GetAuctionBySellerIdRequest;

public interface IAuctionService {
    CompletableFuture<Void> processBid(Bid bid, Auction auction);
    CompletableFuture<Auction> createAuction(CreateAuctionRequest request);
    CompletableFuture<Auction> deleteAuction(Integer auctionId);
    CompletableFuture<Boolean> validateBid(Integer auctionId, Double amount);
    CompletableFuture<Boolean> applySnipeExtension(Auction auction);
    CompletableFuture<Auction> getAuctionDetails(Integer auctionId);
    CompletableFuture<Integer> getSellerId(Integer auctionId);
    CompletableFuture<List<Auction>> getAuctionsBySellerId(GetAuctionBySellerIdRequest request);
    CompletableFuture<List<com.auction.core.dto.auction.PublicAuctionDto>> getPublicAuctions(com.auction.core.dto.auction.GetPublicAuctionsRequest request);
}