package com.auction.server.services;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.dto.auction.CreateAuctionRequest;
import com.auction.core.dto.auction.GetAuctionBySellerIdRequest;
import com.auction.core.dto.auction.GetFeaturedAuctionsRequest;
import com.auction.core.dto.auction.GetPublicAuctionsRequest;
import com.auction.core.dto.auction.PromoteAuctionRequest;
import com.auction.core.dto.auction.PublicAuctionDto;
import com.auction.core.services.IAuctionService;
import com.auction.server.dao.impl.IAuctionDao;
import com.auction.core.exception.auction.InvalidBidException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionService implements IAuctionService {
    private final IAuctionDao auctionDao;
    private final com.auction.server.dao.impl.IItemDao itemDao;
    private final com.auction.server.dao.impl.IUserDao userDao;

    private static class CacheEntry {
        final List<com.auction.core.dto.auction.PublicAuctionDto> data;
        final LocalDateTime expiryTime;

        CacheEntry(
                List<com.auction.core.dto.auction.PublicAuctionDto> data,
                LocalDateTime expiryTime) {
            this.data = data;
            this.expiryTime = expiryTime;
        }
    }

    private final Map<String, CacheEntry> publicAuctionsCache = new ConcurrentHashMap<>();

    public AuctionService(
            IAuctionDao auctionDao,
            com.auction.server.dao.impl.IItemDao itemDao,
            com.auction.server.dao.impl.IUserDao userDao) {
        this.auctionDao = auctionDao;
        this.itemDao = itemDao;
        this.userDao = userDao;
    }

    @Override
    public CompletableFuture<Auction> createAuction(CreateAuctionRequest request) {
        // DB I/O must run on Platform Thread Pool (DBExecutor) — NOT on the calling Virtual Thread.
        // Reason: JDBC Drivers (MySQL Connector/J, MariaDB Client) contain synchronized blocks in
        // their Socket I/O layers, which would Pin the Virtual Thread to a Carrier Thread.
        return CompletableFuture.supplyAsync(
                () -> {
                    Connection conn = null;
                    try {
                        conn = com.auction.server.dao.DBConnection.getConnection();
                        conn.setAutoCommit(false);

                        // 1. Parse category and build Item via Factory (Type-Safe Payload)
                        com.auction.core.products.CategoryType category =
                                com.auction.core.products.CategoryType.valueOf(
                                        request.getItemCategory().trim().toUpperCase());

                        com.auction.core.products.Item item =
                                com.auction.core.products.factory.ItemFactoryProvider.getFactory(
                                                category)
                                        .createItem(
                                                null,
                                                request.getSellerId(),
                                                request.getItemTitle(),
                                                request.getItemDescription(),
                                                request.getItemImageUrl(),
                                                false,
                                                request.getAttributes());

                        // 2. Persist Item on shared connection (Atomic Dual-Write, Step 1/2)
                        boolean itemSaved = itemDao.addItemWithConnection(conn, item);
                        if (!itemSaved || item.getId() == null) {
                            throw new RuntimeException("Saving item failed — item_id not generated");
                        }

                        // 3. Create Auction linked to the generated item_id
                        Auction auction =
                                new Auction(
                                        null,
                                        item.getId(),
                                        request.getStartingPrice(),
                                        request.getBidIncrement(),
                                        request.getStartTime(),
                                        request.getEndTime());

                        // Determine initial status: PENDING if start time is future, else ACTIVE
                        // Auction constructor defaults to PENDING; override if past start time.
                        if (request.getStartTime() != null
                                && !request.getStartTime().isAfter(java.time.LocalDateTime.now())) {
                            auction.setStatus(Auction.Status.ACTIVE);
                        }

                        // 4. Persist Auction on shared connection (Atomic Dual-Write, Step 2/2)
                        boolean auctionSaved = auctionDao.createAuctionWithConnection(conn, auction);
                        if (!auctionSaved) {
                            throw new RuntimeException("Saving auction failed");
                        }

                        conn.commit(); // Both writes succeeded — commit atomically

                        // Lập lịch JVM ngay sau khi tạo phiên thành công
                        if (auction.getStatus() == Auction.Status.PENDING) {
                            AuctionSettlementScheduler.getInstance()
                                    .scheduleAuctionStart(
                                            auction.getId(), auction.getStartTime());
                        } else if (auction.getStatus() == Auction.Status.ACTIVE) {
                            AuctionSettlementScheduler.getInstance()
                                    .scheduleAuctionClose(
                                            auction.getId(), auction.getEndTime());
                        }

                        return auction;

                    } catch (Exception e) {
                        // Defensive Rollback: prevent rollback SQLException from suppressing the
                        // original root cause exception using Suppressed Exception chaining.
                        if (conn != null) {
                            try {
                                if (!conn.isClosed()) {
                                    conn.rollback();
                                }
                            } catch (java.sql.SQLException se) {
                                e.addSuppressed(se); // Preserve rollback failure trace
                            }
                        }
                        throw new RuntimeException("createAuction transaction failed, rolled back", e);
                    } finally {
                        if (conn != null) {
                            try {
                                conn.close();
                            } catch (java.sql.SQLException ignored) {
                                // Best-effort close
                            }
                        }
                    }
                },
                com.auction.server.concurrency.DBExecutor.getExecutor());
    }

    @Override
    public CompletableFuture<Void> processBid(Bid bid, Auction auction) {
        if (bid == null || auction == null) {
            throw new IllegalArgumentException("Bid and Auction must not be null");
        }
        boolean updated = auctionDao.updateAuctionForBid(bid, auction);
        if (!updated) {
            throw new InvalidBidException(
                    "Không thể cập nhật trạng thái đấu giá, có thể do xung đột thầu.");
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Auction getAuctionDetailsForUpdate(Connection conn, Integer auctionId)
            throws SQLException {
        // Gọi đồng bộ trực tiếp — không dùng supplyAsync để giữ nguyên Thread Context
        return auctionDao.getAuctionDetailsForUpdate(conn, auctionId);
    }

    @Override
    public CompletableFuture<Void> processBid(Connection conn, Bid bid, Auction auction) {
        try {
            // Gọi qua kết nối dùng chung của Transaction để đồng bộ (khai báo trong IAuctionDao)
            boolean updated = auctionDao.updateAuctionForBidWithConnection(conn, bid, auction);
            if (!updated) {
                throw new InvalidBidException(
                        "Không thể cập nhật trạng thái đấu giá trong transaction, có thể do xung đột thầu.");
            }
            return CompletableFuture.completedFuture(null);
        } catch (SQLException e) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(
                    new InvalidBidException("Lỗi SQL khi cập nhật thầu: " + e.getMessage()));
            return failed;
        }
    }

    @Override
    public CompletableFuture<Auction> deleteAuction(Integer auctionId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    Auction auction = auctionDao.getAuctionDetails(auctionId);
                    if (auction != null) {
                        boolean deleted = auctionDao.deleteAuction(auction);
                        if (deleted) {
                            // Hủy bỏ mọi tác vụ lập lịch đang chờ của auctionId này
                            AuctionSettlementScheduler.getInstance()
                                    .cancelScheduledTasks(auctionId);
                        }
                    }
                    return auction;
                });
    }

    @Override
    public CompletableFuture<Boolean> applySnipeExtension(Auction auction) {
        return CompletableFuture.supplyAsync(
                () -> applySnipeExtensionSync(auction, LocalDateTime.now()));
    }

    private boolean applySnipeExtensionSync(Auction auction, LocalDateTime bidTime) {
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
    public CompletableFuture<Boolean> validateBid(Integer auctionId, Double amount) {
        return CompletableFuture.supplyAsync(
                () -> {
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
    public CompletableFuture<List<Auction>> getAuctionsBySellerId(
            GetAuctionBySellerIdRequest request) {
        return CompletableFuture.supplyAsync(
                () -> auctionDao.getAuctionsBySellerId(request.getSellerId()));
    }

    @Override
    public CompletableFuture<com.auction.core.dto.auction.AuctionDetailsDto> getAuctionDetails(
            Integer auctionId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    Auction auction = auctionDao.getAuctionDetails(auctionId);
                    if (auction == null) {
                        return null;
                    }

                    com.auction.core.products.Item item = null;
                    if (auction.getItemId() != null) {
                        item = itemDao.findById(auction.getItemId());
                    }

                    com.auction.core.users.User seller = null;
                    if (item != null && item.getSellerId() != null) {
                        seller = userDao.findById(item.getSellerId());
                    }

                    return new com.auction.core.dto.auction.AuctionDetailsDto(
                            auction, item, seller);
                });
    }

    @Override
    public CompletableFuture<Integer> getSellerId(Integer auctionId) {
        return CompletableFuture.completedFuture(auctionDao.getSellerId(auctionId));
    }

    @Override
    public CompletableFuture<List<PublicAuctionDto>> getPublicAuctions(
            GetPublicAuctionsRequest request) {
        return CompletableFuture.supplyAsync(
                () -> {
                    List<String> statuses = normalizeStatuses(request.getStatus());

                    String cacheKey =
                            String.format(
                                    "%d-%d-%s-%b-%b",
                                    request.getPage(),
                                    request.getSize(),
                                    String.join(",", statuses),
                                    request.isIncludeEndingSoon(),
                                    request.isIncludeTrending());

                    CacheEntry entry = publicAuctionsCache.get(cacheKey);
                    if (entry != null && LocalDateTime.now().isBefore(entry.expiryTime)) {
                        return entry.data;
                    }

                    int offset = (request.getPage() - 1) * request.getSize();
                    List<PublicAuctionDto> data =
                            auctionDao.getPublicAuctions(
                                    offset,
                                    request.getSize(),
                                    statuses,
                                    request.isIncludeEndingSoon(),
                                    request.isIncludeTrending());

                    publicAuctionsCache.put(
                            cacheKey, new CacheEntry(data, LocalDateTime.now().plusSeconds(30)));
                    return data;
                });
    }

    private List<String> normalizeStatuses(String status) {
        if (status == null || status.isBlank()) {
            return List.of("ACTIVE", "PENDING");
        }

        List<String> statuses =
                Arrays.stream(status.split(","))
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
        return CompletableFuture.supplyAsync(
                () -> {
                    if (request.getAuctionId() == null || request.getPackageDays() == null) {
                        throw new IllegalArgumentException(
                                "auctionId v\u00e0 packageDays l\u00e0 b\u1eaft bu\u1ed9c.");
                    }
                    int days = request.getPackageDays();
                    if (days != 1 && days != 3) {
                        throw new IllegalArgumentException(
                                "packageDays ch\u1ec9 \u0111\u01b0\u1ee3c l\u00e0 1 ho\u1eb7c 3.");
                    }

                    // Ki\u1ec3m tra quy\u1ec1n s\u1edf h\u1eefu n\u1ebfu kh\u00f4ng ph\u1ea3i Admin
                    // force
                    if (!request.getAdminForce()) {
                        Integer sellerId = auctionDao.getSellerId(request.getAuctionId());
                        if (sellerId == null || !sellerId.equals(request.getSellerId())) {
                            throw new SecurityException(
                                    "B\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n promote auction"
                                            + " n\u00e0y.");
                        }
                    }

                    // Th\u1eddi gian k\u1ebft th\u00fac c\u1ed1 \u0111\u1ecbnh v\u1ec1 00:00
                    // c\u1ee7a (h\u00f4m nay + days + 1) ng\u00e0y
                    // V\u00ed d\u1ee5: promote l\u00fac 15:00 ng\u00e0y 10, g\u00f3i 1 ng\u00e0y ->
                    // featuredUntil = 00:00 ng\u00e0y 12
                    LocalDateTime featuredUntil =
                            LocalDate.now().plusDays(days + 1L).atTime(LocalTime.MIDNIGHT);

                    // Fallback description n\u1ebfu r\u1ed7ng s\u1ebd x\u1eed l\u00fd \u1edf DAO
                    // (d\u00f9ng item description)
                    return auctionDao.promoteAuction(
                            request.getAuctionId(), featuredUntil, request.getShortDescription());
                });
    }

    @Override
    public CompletableFuture<List<PublicAuctionDto>> getFeaturedAuctions(
            GetFeaturedAuctionsRequest request) {
        return CompletableFuture.supplyAsync(
                () -> {
                    int limit =
                            (request != null && request.getLimit() > 0) ? request.getLimit() : 5;
                    return auctionDao.getFeaturedAuctions(limit);
                });
    }

    @Override
    public CompletableFuture<List<PublicAuctionDto>> getAllAuctionsForAdmin(
            String status, int page, int size) {
        return CompletableFuture.supplyAsync(
                () -> {
                    List<String> statuses = normalizeStatuses(status);
                    int offset = Math.max(0, (page - 1)) * size;
                    return auctionDao.getAllAuctionsForAdmin(statuses, offset, size);
                });
    }
}
