package com.auction.server.services;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.server.dao.impl.IBidDao;
import com.auction.server.dao.impl.IUserDao;
import com.auction.core.dto.bid.GetBidByAuctionIdRequest;
import com.auction.core.dto.bid.GetBidByBidderIdRequest;
import com.auction.core.dto.bid.PlaceBid;
import com.auction.core.services.IAuctionService;
import com.auction.core.services.IBidService;
import com.auction.core.users.User;

public class BidService implements IBidService {
    private static final int BID_TIMEOUT_SECONDS = 5;

    private final IBidDao bidDao;
    private final IAuctionService auctionService;
    private final IUserDao userDao;
    private final BidQueueManager bidQueueManager;

    public BidService(IBidDao bid, IAuctionService auctionService, IUserDao userDao, BidQueueManager bidQueueManager) {
        this.bidDao = bid;
        this.auctionService = auctionService;
        this.userDao = userDao;
        this.bidQueueManager = bidQueueManager;
    }

    @Override
    public CompletableFuture<Bid> placeBid(PlaceBid request) {
        // Pha 1: Pre-validation (parallel — no serialization needed)
        return CompletableFuture.supplyAsync(() -> preValidate(request))
            .thenCompose(task -> {
                // Pha 2: Submit to per-auction queue (serial per auction)
                return bidQueueManager.submitBid(task);
            })
            .orTimeout(BID_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .exceptionally(ex -> {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                if (cause instanceof TimeoutException) {
                    throw new RuntimeException("Bid processing timed out, please retry");
                }
                if (cause instanceof IllegalArgumentException || cause instanceof IllegalStateException) {
                    throw (RuntimeException) cause;
                }
                throw new RuntimeException(cause.getMessage(), cause);
            });
    }

    /**
     * Pha 1: Pre-validate bid request on caller thread.
     * Quick checks that don't need serialization: user exists, auction active, shill bidding, deposit hold.
     * Returns a BidTask ready for queue submission.
     */
    private BidTask preValidate(PlaceBid request) {
        if (request == null) throw new IllegalArgumentException("Request is required");
        if (auctionService == null || userDao == null || bidDao == null) {
            throw new IllegalStateException("Services are not initialized");
        }

        // Validate User
        User user = userDao.findById(request.getBidderId());
        if (user == null) throw new IllegalArgumentException("User not found");

        // Validate Auction
        Auction auction = auctionService.getAuctionDetails(request.getAuctionId()).join();
        if (auction == null) throw new IllegalArgumentException("Auction not found");
        if (auction.getStatus() != Auction.Status.ACTIVE) {
            throw new IllegalArgumentException("Auction is not active");
        }

        // Shill Bidding check (seller cannot bid on own auction)
        Integer sellerId = auctionService.getSellerId(request.getAuctionId()).join();
        if (sellerId != null && sellerId.equals(request.getBidderId())) {
            throw new IllegalArgumentException("Seller cannot bid on their own auction");
        }

        // Validate amount
        double amount = request.getAmount();
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Invalid bid amount");
        }

        if (java.time.LocalDateTime.now().isAfter(auction.getEndTime())) {
            throw new IllegalArgumentException("Auction has already ended");
        }

        // Hold deposit if first bid
        boolean hasBidBefore = bidDao.hasBid(request.getAuctionId(), request.getBidderId());
        if (!hasBidBefore) {
            double depositAmount = auction.getStartingPrice() * 0.3;
            boolean held = userDao.holdBalance(user.getId(), depositAmount);
            if (!held) throw new IllegalArgumentException("Insufficient balance for deposit");
        }

        // Build task with pre-validated snapshot
        CompletableFuture<Bid> resultFuture = new CompletableFuture<>();
        return new BidTask(request, resultFuture, auction, !hasBidBefore);
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
