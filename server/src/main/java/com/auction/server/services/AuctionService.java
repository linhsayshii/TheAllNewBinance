package com.auction.server.services;

import java.time.LocalDateTime;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.dao.IAuctionDao;
import com.auction.core.products.Item;
import com.auction.core.services.IAuctionService;

public class AuctionService implements IAuctionService {
    private final IAuctionDao auctionDao;

    public AuctionService(IAuctionDao auctionDao) {
        this.auctionDao = auctionDao;
    }

    @Override
    public Auction createAuction(Item item, Double startingPrice, Double bidIncrement, LocalDateTime startTime, LocalDateTime endTime) {
        Auction auction = new Auction(null, item.getId(), startingPrice, bidIncrement, startTime, endTime);
        auctionDao.createAuction(auction);
        return auction;
    }

    @Override
    public void processBid(Bid bid, Auction auction) {
        if (bid == null || auction == null) {
            throw new IllegalArgumentException("Bid and Auction must not be null");
        }
        
        // Snipe extension logic is now safely handled within the DB lock in AuctionDao
        boolean updated = auctionDao.updateAuctionForBid(bid, auction);
        if (!updated) {
            throw new IllegalStateException("Failed to update auction bid state, possibly concurrency issue.");
        }
    }

    @Override
    public Auction deleteAuction(Integer auctionId) {
        Auction auction = auctionDao.getAuctionDetails(auctionId);
        if (auction != null) {
            auctionDao.deleteAuction(auction);
        }
        return auction;
    }

    @Override
    public boolean applySnipeExtension(Auction auction) {
        return applySnipeExtension(auction, LocalDateTime.now());
    }

    private boolean applySnipeExtension(Auction auction, LocalDateTime bidTime) {
        if (auction == null) {
            return false;
        }

        boolean isExtended = auction.applySnipeExtension(bidTime);
        if (isExtended) {
            auctionDao.extendAuction(auction);
        }
        return isExtended;
    }

    @Override
    public boolean validateBid(Integer auctionId, Double amount) {
        Auction auction = auctionDao.getAuctionDetails(auctionId);
        if (auction == null) {
            throw new IllegalArgumentException("Auction not found");
        }
        double currentPrice = auctionDao.getCurrentPrice(auctionId);
        return amount >= (currentPrice + auction.getBidIncrement()) && LocalDateTime.now().isBefore(auction.getEndTime());
    }

    @Override
    public Auction getAuctionDetails(Integer auctionId) {
        return auctionDao.getAuctionDetails(auctionId);
    }

    @Override
    public Integer getSellerId(Integer auctionId) {
        return auctionDao.getSellerId(auctionId);
    }
}
