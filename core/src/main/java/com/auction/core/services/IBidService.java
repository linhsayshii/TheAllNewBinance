package com.auction.core.services;

import java.util.List;

import com.auction.core.auction.Bid;
import com.auction.core.dto.BidService.GetBidByAuctionIdRequest;
import com.auction.core.dto.BidService.GetBidByBidderIdRequest;
import com.auction.core.dto.BidService.PlaceBid;
import com.auction.core.users.User;

public interface IBidService {
    Bid placeBid(PlaceBid request);
    boolean validateUserBid(Integer auctionId, User user);
    void automaticallyPlaceBid(Integer auctionId, double increment, double maxAmount);
    List<Bid> getBidsByAuctionId(GetBidByAuctionIdRequest request);
    List<Bid> getBidsByBidderId(GetBidByBidderIdRequest request);
}
