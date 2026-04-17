package com.auction.server.services;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.dao.IBidDao;
import com.auction.core.dao.IUserDao;
import com.auction.core.dto.bid.GetBidByAuctionIdRequest;
import com.auction.core.dto.bid.GetBidByBidderIdRequest;
import com.auction.core.dto.bid.PlaceBid;
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
    public CompletableFuture<Bid> placeBid(PlaceBid request) {
        return CompletableFuture.supplyAsync(() -> {
            if (request == null) throw new IllegalArgumentException("Request is required");
            if (auctionService == null || userDao == null || bidDao == null) {
                throw new IllegalStateException("Services are not initialized");
            }

            try {
                DBConnection.beginTransaction();

                // Bước 1: Validate User & Check initial Application-level checks
                User user = userDao.findById(request.getBidderId());
                if (user == null) throw new IllegalArgumentException("User not found");

                // .join() để chờ kết quả bất đồng bộ trong cùng thread pool
                Auction auction = auctionService.getAuctionDetails(request.getAuctionId()).join();
                if (auction == null) throw new IllegalArgumentException("Auction not found");
                if (auction.getStatus() != Auction.Status.ACTIVE) {
                    throw new IllegalArgumentException("Auction is not active");
                }

                // Kiểm tra Shill Bidding (người bán không được tự mua)
                Integer sellerId = auctionService.getSellerId(request.getAuctionId()).join();
                if (sellerId != null && sellerId.equals(request.getBidderId())) {
                    throw new IllegalArgumentException("Seller cannot bid on their own auction");
                }

                double amount = request.getAmount();
                if (!Double.isFinite(amount) || amount <= 0) {
                    throw new IllegalArgumentException("Invalid bid amount");
                }

                if (java.time.LocalDateTime.now().isAfter(auction.getEndTime())) {
                    throw new IllegalArgumentException("Auction has already ended");
                }

                // Xử lý cọc (Hold Balance) chỉ khi user chưa đặt bid nào trong phiên này
                boolean hasBidBefore = bidDao.hasBid(request.getAuctionId(), request.getBidderId());
                if (!hasBidBefore) {
                    double depositAmount = auction.getStartingPrice() * 0.3;
                    boolean held = userDao.holdBalance(user.getId(), depositAmount);
                    if (!held) throw new IllegalArgumentException("Insufficient balance for deposit");
                }

                // Bước 2: Cập nhật thông tin Auction với Atomic check
                Bid bid = new Bid(null, request.getAuctionId(), request.getBidderId(), amount);
                auctionService.processBid(bid, auction).join();

                // Bước 3: Save bid vào Database sau khi MỌI thứ ok
                boolean saved = bidDao.saveBid(bid);
                if (!saved) throw new IllegalStateException("Cannot persist bid");

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
        });
    }

    @Override
    public CompletableFuture<Boolean> validateUserBid(Integer auctionId, User user) {
        return auctionService.getAuctionDetails(auctionId).thenApply(auction -> {
            if (auction == null) return false;
            return user.getBalance() >= (auction.getStartingPrice() * 0.3);
        });
    }

    @Override
    public CompletableFuture<Void> automaticallyPlaceBid(Integer auctionId, double increment, double maxAmount) {
        // implementation waiting
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<Bid>> getBidsByAuctionId(GetBidByAuctionIdRequest request) {
        return CompletableFuture.supplyAsync(() -> bidDao.findByAuctionId(request.getAuctionId()));
    }

    @Override
    public CompletableFuture<List<Bid>> getBidsByBidderId(GetBidByBidderIdRequest request) {
        return CompletableFuture.supplyAsync(() -> bidDao.findByBidderId(request.getBidderId()));
    }
}
