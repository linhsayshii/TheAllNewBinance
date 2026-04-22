package com.auction.server.services;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.dto.auction.CreateAuctionRequest;
import com.auction.core.dto.auction.GetAuctionBySellerIdRequest;
import com.auction.core.dto.auction.PublicAuctionDto;
import com.auction.core.services.IAuctionService;
import com.auction.server.dao.IAuctionDao;

public class AuctionService implements IAuctionService {
    private final IAuctionDao auctionDao;

    private static class CacheEntry {
        final List<com.auction.core.dto.auction.PublicAuctionDto> data;
        final LocalDateTime expiryTime;

        CacheEntry(List<com.auction.core.dto.auction.PublicAuctionDto> data, LocalDateTime expiryTime) {
            this.data = data;
            this.expiryTime = expiryTime;
        }
    }

    private final Map<String, CacheEntry> publicAuctionsCache = new ConcurrentHashMap<>();

    public AuctionService(IAuctionDao auctionDao) {
        this.auctionDao = auctionDao;
    }

    @Override
    public CompletableFuture<Auction> createAuction(CreateAuctionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            Auction auction = new Auction(null, request.getItemId(), request.getStartingPrice(),
                    request.getBidIncrement(),
                    request.getStartTime(), request.getEndTime());
            auctionDao.createAuction(auction);
            return auction;
        });
    }

    @Override
    public CompletableFuture<Void> processBid(Bid bid, Auction auction) {
        return CompletableFuture.runAsync(() -> {
            if (bid == null || auction == null) {
                throw new IllegalArgumentException("Bid and Auction must not be null");
            }
            boolean updated = auctionDao.updateAuctionForBid(bid, auction);
            if (!updated) {
                throw new IllegalStateException("Failed to update auction bid state, possibly concurrency issue.");
            }
        });
    }

    @Override
    public CompletableFuture<Auction> deleteAuction(Integer auctionId) {
        return CompletableFuture.supplyAsync(() -> {
            Auction auction = auctionDao.getAuctionDetails(auctionId);
            if (auction != null) {
                auctionDao.deleteAuction(auction);
            }
            return auction;
        });
    }

    @Override
    public CompletableFuture<Boolean> applySnipeExtension(Auction auction) {
        return CompletableFuture.supplyAsync(() -> applySnipeExtensionSync(auction, LocalDateTime.now()));
    }

    private boolean applySnipeExtensionSync(Auction auction, LocalDateTime bidTime) {
        if (auction == null)
            return false;
        boolean isExtended = auction.applySnipeExtension(bidTime);
        if (isExtended) {
            auctionDao.extendAuction(auction);
        }
        return isExtended;
    }

    @Override
    public CompletableFuture<Boolean> validateBid(Integer auctionId, Double amount) {
        return CompletableFuture.supplyAsync(() -> {
            Auction auction = auctionDao.getAuctionDetails(auctionId);
            if (auction == null) {
                throw new IllegalArgumentException("Auction not found");
            }
            double currentPrice = auctionDao.getCurrentPrice(auctionId);
            return amount >= (currentPrice + auction.getBidIncrement())
                    && LocalDateTime.now().isBefore(auction.getEndTime());
        });
    }

    @Override
    public CompletableFuture<List<Auction>> getAuctionsBySellerId(GetAuctionBySellerIdRequest request) {
        return CompletableFuture.supplyAsync(() -> auctionDao.getAuctionsBySellerId(request.getSellerId()));
    }

    @Override
    public CompletableFuture<Auction> getAuctionDetails(Integer auctionId) {
        return CompletableFuture.supplyAsync(() -> auctionDao.getAuctionDetails(auctionId));
    }

    @Override
    public CompletableFuture<Integer> getSellerId(Integer auctionId) {
        return CompletableFuture.supplyAsync(() -> auctionDao.getSellerId(auctionId));
    }

    @Override
    public CompletableFuture<List<PublicAuctionDto>> getPublicAuctions(
            com.auction.core.dto.auction.GetPublicAuctionsRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> statuses = normalizeStatuses(request.getStatus());

            String cacheKey = String.format("%d-%d-%s-%b-%b",
                request.getPage(), request.getSize(), String.join(",", statuses),
                    request.isIncludeEndingSoon(), request.isIncludeTrending());

            CacheEntry entry = publicAuctionsCache.get(cacheKey);
            if (entry != null && LocalDateTime.now().isBefore(entry.expiryTime)) {return entry.data; }

            int offset = (request.getPage() - 1) * request.getSize();
            List<PublicAuctionDto> data = auctionDao.getPublicAuctions(
                    offset,
                    request.getSize(),
                    statuses,
                    request.isIncludeEndingSoon(),
                    request.isIncludeTrending());

            publicAuctionsCache.put(cacheKey, new CacheEntry(data, LocalDateTime.now().plusSeconds(30)));
            return data;
        });
    }

    private List<String> normalizeStatuses(String status) {
        if (status == null || status.isBlank()) {
            return List.of("ACTIVE", "PENDING");
        }

        List<String> statuses = Arrays.stream(status.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();

        if (statuses.isEmpty()) {
            return List.of("ACTIVE", "PENDING");
        }
        return statuses;
    }
}
