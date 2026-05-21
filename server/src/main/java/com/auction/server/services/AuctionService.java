package com.auction.server.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.dto.auction.CreateAuctionRequest;
import com.auction.core.dto.auction.GetAuctionBySellerIdRequest;
import com.auction.core.dto.auction.GetFeaturedAuctionsRequest;
import com.auction.core.dto.auction.PromoteAuctionRequest;
import com.auction.core.dto.auction.PublicAuctionDto;
import com.auction.core.services.IAuctionService;
import com.auction.server.dao.impl.IAuctionDao;

public class AuctionService implements IAuctionService {
    private final IAuctionDao auctionDao;
    private final com.auction.server.dao.impl.IItemDao itemDao;
    private final com.auction.server.dao.impl.IUserDao userDao;

    private static class CacheEntry {
        final List<com.auction.core.dto.auction.PublicAuctionDto> data;
        final LocalDateTime expiryTime;

        CacheEntry(List<com.auction.core.dto.auction.PublicAuctionDto> data, LocalDateTime expiryTime) {
            this.data = data;
            this.expiryTime = expiryTime;
        }
    }

    private final Map<String, CacheEntry> publicAuctionsCache = new ConcurrentHashMap<>();

    public AuctionService(IAuctionDao auctionDao, com.auction.server.dao.impl.IItemDao itemDao, com.auction.server.dao.impl.IUserDao userDao) {
        this.auctionDao = auctionDao;
        this.itemDao = itemDao;
        this.userDao = userDao;
    }

    @Override
    public CompletableFuture<Auction> createAuction(CreateAuctionRequest request) {
        // THỰC TẾ: Đây vẫn là tác vụ độc lập nên supplyAsync là hợp lệ
        return CompletableFuture.supplyAsync(() -> {
            Auction auction = new Auction(null, null, request.getStartingPrice(),
                    request.getBidIncrement(),
                    request.getStartTime(), request.getEndTime());
            auctionDao.createAuction(auction);
            return auction;
        });
    }

    @Override
    public CompletableFuture<Void> processBid(Bid bid, Auction auction) {
        if (bid == null || auction == null) {
            throw new IllegalArgumentException("Bid and Auction must not be null");
        }
        boolean updated = auctionDao.updateAuctionForBid(bid, auction);
        if (!updated) {
            throw new IllegalStateException("Failed to update auction bid state, possibly concurrency issue.");
        }
        return CompletableFuture.completedFuture(null);
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
    public CompletableFuture<com.auction.core.dto.auction.AuctionDetailsDto> getAuctionDetails(Integer auctionId) {
        return CompletableFuture.supplyAsync(() -> {
            Auction auction = auctionDao.getAuctionDetails(auctionId);
            if (auction == null) return null;
            
            com.auction.core.products.Item item = null;
            if (auction.getItemId() != null) {
                item = itemDao.findById(auction.getItemId());
            }
            
            com.auction.core.users.User seller = null;
            if (item != null && item.getSellerId() != null) {
                seller = userDao.findById(item.getSellerId());
            }
            
            return new com.auction.core.dto.auction.AuctionDetailsDto(auction, item, seller);
        });
    }

    @Override
    public CompletableFuture<Integer> getSellerId(Integer auctionId) {
        return CompletableFuture.completedFuture(auctionDao.getSellerId(auctionId));
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
            if (entry != null && LocalDateTime.now().isBefore(entry.expiryTime)) {
                return entry.data;
            }

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

    @Override
    public CompletableFuture<Boolean> promoteAuction(PromoteAuctionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            if (request.getAuctionId() == null || request.getPackageDays() == null) {
                throw new IllegalArgumentException("auctionId v\u00e0 packageDays l\u00e0 b\u1eaft bu\u1ed9c.");
            }
            int days = request.getPackageDays();
            if (days != 1 && days != 3) {
                throw new IllegalArgumentException("packageDays ch\u1ec9 \u0111\u01b0\u1ee3c l\u00e0 1 ho\u1eb7c 3.");
            }

            // Ki\u1ec3m tra quy\u1ec1n s\u1edf h\u1eefu n\u1ebfu kh\u00f4ng ph\u1ea3i Admin force
            if (!request.getAdminForce()) {
                Integer sellerId = auctionDao.getSellerId(request.getAuctionId());
                if (sellerId == null || !sellerId.equals(request.getSellerId())) {
                    throw new SecurityException("B\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n promote auction n\u00e0y.");
                }
            }

            // Th\u1eddi gian k\u1ebft th\u00fac c\u1ed1 \u0111\u1ecbnh v\u1ec1 00:00 c\u1ee7a (h\u00f4m nay + days + 1) ng\u00e0y
            // V\u00ed d\u1ee5: promote l\u00fac 15:00 ng\u00e0y 10, g\u00f3i 1 ng\u00e0y -> featuredUntil = 00:00 ng\u00e0y 12
            LocalDateTime featuredUntil = LocalDate.now().plusDays(days + 1L).atTime(LocalTime.MIDNIGHT);

            // Fallback description n\u1ebfu r\u1ed7ng s\u1ebd x\u1eed l\u00fd \u1edf DAO (d\u00f9ng item description)
            return auctionDao.promoteAuction(request.getAuctionId(), featuredUntil, request.getShortDescription());
        });
    }

    @Override
    public CompletableFuture<List<PublicAuctionDto>> getFeaturedAuctions(GetFeaturedAuctionsRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            int limit = (request != null && request.getLimit() > 0) ? request.getLimit() : 5;
            return auctionDao.getFeaturedAuctions(limit);
        });
    }

    @Override
    public CompletableFuture<List<PublicAuctionDto>> getAllAuctionsForAdmin(String status, int page, int size) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> statuses = normalizeStatuses(status);
            int offset = Math.max(0, (page - 1)) * size;
            return auctionDao.getAllAuctionsForAdmin(statuses, offset, size);
        });
    }
}
