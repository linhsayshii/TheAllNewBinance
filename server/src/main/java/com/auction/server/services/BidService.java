package com.auction.server.services;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.dto.bid.GetBidByAuctionIdRequest;
import com.auction.core.dto.bid.GetBidByBidderIdRequest;
import com.auction.core.dto.bid.PlaceBid;
import com.auction.core.exception.DomainException;
import com.auction.core.exception.auction.AuctionClosedException;
import com.auction.core.exception.auction.InvalidBidException;
import com.auction.core.exception.auction.ShillBiddingForbiddenException;
import com.auction.core.exception.wallet.InsufficientBalanceException;
import com.auction.core.services.IAuctionService;
import com.auction.core.services.IBidService;
import com.auction.core.users.User;
import com.auction.server.dao.impl.IBidDao;
import com.auction.server.dao.impl.IUserDao;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BidService implements IBidService {
    private static final int BID_TIMEOUT_SECONDS = 5;

    private final IBidDao bidDao;
    private final IAuctionService auctionService;
    private final IUserDao userDao;
    private final BidQueueManager bidQueueManager;

    public BidService(
            IBidDao bid,
            IAuctionService auctionService,
            IUserDao userDao,
            BidQueueManager bidQueueManager) {
        this.bidDao = bid;
        this.auctionService = auctionService;
        this.userDao = userDao;
        this.bidQueueManager = bidQueueManager;
    }

    @Override
    public CompletableFuture<Bid> placeBid(PlaceBid request) {
        // Pha 1: Pre-validation (parallel — no serialization needed)
        return CompletableFuture.supplyAsync(() -> preValidate(request))
                .thenCompose(
                        task -> {
                            // Pha 2: Submit to per-auction queue (serial per auction)
                            return bidQueueManager.submitBid(task);
                        })
                .orTimeout(BID_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(
                        ex -> {
                            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                            if (cause instanceof TimeoutException) {
                                throw new RuntimeException(
                                        "Bid processing timed out, please retry");
                            }
                            if (cause instanceof DomainException domainEx) {
                                throw domainEx;
                            }
                            throw new RuntimeException(cause.getMessage(), cause);
                        });
    }

    /**
     * Pha 1: Pre-validate bid request on caller thread. Quick checks that don't need serialization:
     * user exists, auction active, shill bidding, deposit hold. Returns a BidTask ready for queue
     * submission.
     */
    private BidTask preValidate(PlaceBid request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }
        if (auctionService == null || userDao == null || bidDao == null) {
            throw new IllegalStateException("Services are not initialized");
        }

        // Validate User
        User user = userDao.findById(request.getBidderId());
        if (user == null) {
            throw new InvalidBidException("User not found");
        }

        // .join() to wait for asynchronous results in the same thread pool
        com.auction.core.dto.auction.AuctionDetailsDto details =
                auctionService.getAuctionDetails(request.getAuctionId()).join();
        if (details == null || details.getAuction() == null) {
            throw new AuctionClosedException("Auction not found");
        }
        Auction auction = details.getAuction();

        if (auction.getStatus() != Auction.Status.ACTIVE) {
            throw new AuctionClosedException("Auction is not active");
        }

        // Shill Bidding check: seller cannot bid on their own auction
        Integer sellerId = auctionService.getSellerId(request.getAuctionId()).join();
        if (sellerId != null && sellerId.equals(request.getBidderId())) {
            throw new ShillBiddingForbiddenException();
        }

        // Validate amount
        double amount = request.getAmount();
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new InvalidBidException("Invalid bid amount");
        }

        if (java.time.LocalDateTime.now().isAfter(auction.getEndTime())) {
            throw new AuctionClosedException("Auction has already ended");
        }

        // Kiểm tra số dư ví khả dụng ban đầu (Không thực hiện đóng băng tại đây để tránh rò rỉ cọc)
        // Việc khóa cọc thực sự được thực hiện trong Transaction tuần tự của BidQueueManager
        boolean hasBidBefore = bidDao.hasBid(request.getAuctionId(), request.getBidderId());
        double requiredBalance = amount;
        if (!hasBidBefore) {
            requiredBalance += (auction.getStartingPrice() * 0.3);
        }
        if (user.getBalance().compareTo(BigDecimal.valueOf(requiredBalance)) < 0) {
            throw new InsufficientBalanceException(
                    "Số dư tài khoản không đủ để đặt thầu"
                            + " (bao gồm cả tiền cọc nếu là lượt đầu).");
        }

        // Build task with pre-validated snapshot
        CompletableFuture<Bid> resultFuture = new CompletableFuture<>();
        return new BidTask(request, resultFuture, auction, !hasBidBefore);
    }

    @Override
    public CompletableFuture<Boolean> validateUserBid(Integer auctionId, User user) {
        return auctionService
                .getAuctionDetails(auctionId)
                .thenApply(
                        details -> {
                            if (details == null || details.getAuction() == null) {
                                return false;
                            }
                            Auction auction = details.getAuction();
                            BigDecimal required = BigDecimal.valueOf(auction.getStartingPrice() * 0.3);
                            return user.getBalance().compareTo(required) >= 0;
                        });
    }

    @Override
    public CompletableFuture<Void> automaticallyPlaceBid(
            Integer auctionId, double increment, double maxAmount) {
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
