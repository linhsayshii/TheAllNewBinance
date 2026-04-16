package com.auction.core.dao;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import java.time.LocalDateTime;

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
}
