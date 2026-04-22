package com.auction.server.dao;

import java.util.List;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;

public interface IAuctionDao {
    boolean createAuction(Auction auction);
    boolean updateAuctionInformation(Auction auction);
    boolean deleteAuction(Auction auction);
    boolean extendAuction(Auction auction);
    Auction getAuctionDetails(Integer auctionId);
    double getCurrentPrice(Integer auctionId);
    void updateCurrentPrice(Bid bid);
    boolean updateAuctionForBid(Bid bid, Auction auction);
    Integer getSellerId(Integer auctionId);
    List<Auction> getAuctionsBySellerId(Integer sellerId);
    List<com.auction.core.dto.auction.PublicAuctionDto> getPublicAuctions(
            int offset,
            int limit,
            List<String> statuses,
            boolean includeEndingSoon,
            boolean includeTrending);
}
