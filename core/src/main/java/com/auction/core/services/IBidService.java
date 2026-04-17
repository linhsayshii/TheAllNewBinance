package com.auction.core.services;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.auction.core.auction.Bid;
import com.auction.core.dto.bid.GetBidByAuctionIdRequest;
import com.auction.core.dto.bid.GetBidByBidderIdRequest;
import com.auction.core.dto.bid.PlaceBid;
import com.auction.core.users.User;

public interface IBidService {
    CompletableFuture<Bid> placeBid(PlaceBid request);
    CompletableFuture<Boolean> validateUserBid(Integer auctionId, User user);
    CompletableFuture<Void> automaticallyPlaceBid(Integer auctionId, double increment, double maxAmount);
    CompletableFuture<List<Bid>> getBidsByAuctionId(GetBidByAuctionIdRequest request);
    CompletableFuture<List<Bid>> getBidsByBidderId(GetBidByBidderIdRequest request);
}
