package com.auction.server.services;

import java.util.List;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.dao.IBidDao;
import com.auction.core.dao.IUserDao;
import com.auction.core.dto.BidService.GetBidByAuctionID;
import com.auction.core.dto.BidService.GetBidByBidderID;
import com.auction.core.dto.BidService.PlaceBid;
import com.auction.core.services.IAuctionService;
import com.auction.core.services.IBidService;
import com.auction.core.users.User;
import com.auction.server.dao.DBConnection;

public class BidService implements IBidService {
    private final IBidDao bidDao;
    private final IAuctionService auctionService;
    private final IUserDao userDao;

    public BidService(IBidDao bid, IAuctionService auctionService, IUserDao userDao) {
        this.bidDao = bid;
        this.auctionService = auctionService;
        this.userDao = userDao;
    }


    @Override
    public Bid placeBid(PlaceBid request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }
        if (auctionService == null || userDao == null || bidDao == null) {
            throw new IllegalStateException("Services are not initialized");
        }
        
        try {
            DBConnection.beginTransaction();

            // Bước 1: Validate User & Check initial Application-level checks
            User user = userDao.findById(request.getBidderId());
            if (user == null) {
                throw new IllegalArgumentException("User not found");
            }
            Auction auction = auctionService.getAuctionDetails(request.getAuctionId());
            if (auction == null) {
                throw new IllegalArgumentException("Auction not found");
            }
            if (auction.getStatus() != Auction.Status.ACTIVE) {
                throw new IllegalArgumentException("Auction is not active");
            }

            double amount = request.getAmount();
            if (!Double.isFinite(amount) || amount <= 0) {
                throw new IllegalArgumentException("Invalid bid amount");
            }

            if (!auctionService.validateBid(request.getAuctionId(), amount)) {
                throw new IllegalArgumentException("Invalid bid amount or auction has already ended");
            }

            // Bước 2: Cập nhật thông tin Auction với Atomic check
            Bid bid = new Bid(null, request.getAuctionId(), request.getBidderId(), amount);
            auctionService.processBid(bid);

            // Bước 3: Save bid vào Database sau khi MỌI thứ ok
            boolean saved = bidDao.saveBid(bid);
            if (!saved) {
                throw new IllegalStateException("Cannot persist bid");
            }

            DBConnection.commitTransaction();
            return bid;
        } catch (IllegalArgumentException | IllegalStateException e) {
            DBConnection.rollbackTransaction();
            throw e;
        } catch (Exception e) {
            DBConnection.rollbackTransaction();
            throw new RuntimeException("Transaction error while placing bid", e);
        } finally {
            DBConnection.closeConnection();
        }
    }

    @Override
    public boolean validateUserBid(Integer auctionId, User user) {
        Auction auction = auctionService.getAuctionDetails(auctionId);
        if (auction == null) {
            return false;
        }
        return user.getBalance() >= (auction.getStartingPrice() * 0.3); //giá cọc
    }
    @Override
    public void automaticallyPlaceBid(Integer auctionId, double increment, double maxAmount) {
        //implemention waiting
    }
    @Override
    public List<Bid> getBidsByAuctionId(GetBidByAuctionID request) {
        return bidDao.findByAuctionId(request.getAuctionId());
    }
    @Override
    public List<Bid> getBidsByBidderId(GetBidByBidderID request) {
        return bidDao.findByBidderId(request.getBidderId());
    }

}
