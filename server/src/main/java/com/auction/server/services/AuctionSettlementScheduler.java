package com.auction.server.services;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.dto.auction.AuctionDetailsDto;
import com.auction.core.dto.auction.PublicAuctionDto;
import com.auction.core.exception.auction.AuctionActivationException;
import com.auction.core.exception.auction.AuctionSettlementException;
import com.auction.core.protocol.EventType;
import com.auction.core.users.User;
import com.auction.server.dao.DBConnection;
import com.auction.server.dao.impl.IAuctionDao;
import com.auction.server.dao.impl.IBidDao;
import com.auction.server.dao.impl.IUserDao;
import com.auction.server.network.BroadcastBroker;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Lập lịch Event-Driven quản lý trọn vẹn vòng đời đấu giá trên Virtual Threads.
 *
 * <p>Khi khởi động Server, nạp tất cả Auction ACTIVE (lập lịch đóng phiên) và PENDING (lập lịch mở
 * phiên) để đảm bảo 100% crash recovery sau sự cố restart.
 *
 * <p>Pha 1 Mở phiên (PENDING → ACTIVE): Kết toán nguyên tử trạng thái với khóa bi quan FOR UPDATE,
 * sau đó tự động lập lịch Pha 2.
 *
 * <p>Pha 2 Đóng phiên (ACTIVE → ENDED/CANCELLED): Kết toán nguyên tử Winner/Seller với khóa bi quan
 * FOR UPDATE, sau đó hoàn cọc bất đồng bộ theo lô 100 Loser.
 */
public class AuctionSettlementScheduler {

    private static volatile AuctionSettlementScheduler instance;

    private final IAuctionDao auctionDao;
    private final IBidDao bidDao;
    private final IUserDao userDao;

    private final ScheduledThreadPoolExecutor executor =
            new ScheduledThreadPoolExecutor(
                    4,
                    r -> {
                        Thread t = new Thread(r, "auction-settle-worker");
                        t.setDaemon(true);
                        return t;
                    });

    /** Timer map cho tác vụ mở phiên (PENDING → ACTIVE). */
    private final ConcurrentHashMap<Integer, ScheduledFuture<?>> activeStartTasks =
            new ConcurrentHashMap<>();

    /** Timer map cho tác vụ đóng phiên (ACTIVE → ENDED/CANCELLED). */
    private final ConcurrentHashMap<Integer, ScheduledFuture<?>> activeCloseTasks =
            new ConcurrentHashMap<>();

    public AuctionSettlementScheduler(IAuctionDao auctionDao, IBidDao bidDao, IUserDao userDao) {
        this.auctionDao = auctionDao;
        this.bidDao = bidDao;
        this.userDao = userDao;
        instance = this;
        executor.setRemoveOnCancelPolicy(true);
    }

    public static AuctionSettlementScheduler getInstance() {
        return instance;
    }

    /**
     * Khởi động: nạp tất cả Auction ACTIVE (lập lịch đóng phiên) và PENDING (lập lịch mở phiên) dựa
     * trên dữ liệu thực tế trong DB, đảm bảo hồi phục hoàn toàn sau crash/restart.
     */
    public void start() {
        try {
            System.out.println(
                    "[AuctionSettlementScheduler] Initializing active and pending auction"
                            + " timers...");

            // 1. Phục hồi và lập lịch đóng phiên cho các Auction ACTIVE
            com.auction.core.dto.auction.GetPublicAuctionsRequest activeReq =
                    new com.auction.core.dto.auction.GetPublicAuctionsRequest();
            activeReq.setStatus("ACTIVE");
            activeReq.setIncludeEndingSoon(false);
            activeReq.setIncludeTrending(false);

            List<PublicAuctionDto> activeAuctions =
                    auctionDao.getPublicAuctions(0, 10000, activeReq);
            for (PublicAuctionDto dto : activeAuctions) {
                scheduleAuctionClose(dto.getAuctionId(), dto.getEndTime());
            }

            // 2. Phục hồi và lập lịch mở phiên cho các Auction PENDING
            com.auction.core.dto.auction.GetPublicAuctionsRequest pendingReq =
                    new com.auction.core.dto.auction.GetPublicAuctionsRequest();
            pendingReq.setStatus("PENDING");
            pendingReq.setIncludeEndingSoon(false);
            pendingReq.setIncludeTrending(false);

            List<PublicAuctionDto> pendingAuctions =
                    auctionDao.getPublicAuctions(0, 10000, pendingReq);
            for (PublicAuctionDto dto : pendingAuctions) {
                scheduleAuctionStart(dto.getAuctionId(), dto.getStartTime());
            }

            System.out.println(
                    "[AuctionSettlementScheduler] Loaded "
                            + activeAuctions.size()
                            + " active timers and "
                            + pendingAuctions.size()
                            + " pending timers.");
        } catch (Exception e) {
            System.err.println(
                    "[AuctionSettlementScheduler] Startup initialization failed: "
                            + e.getMessage());
        }
    }

    public void stop() {
        executor.shutdownNow();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // MỞ PHIÊN: PENDING → ACTIVE
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Lập lịch mở phiên đấu giá cho Auction PENDING.
     *
     * <p>Nếu startTime đã qua (overdue sau crash/restart), kích hoạt ngay lập tức trên Virtual
     * Thread. Ngược lại, lập lịch trì hoãn đúng bằng khoảng cách thời gian.
     */
    public synchronized void scheduleAuctionStart(Integer auctionId, LocalDateTime startTime) {
        // Hủy bỏ start timer cũ nếu có để tránh lập lịch trùng lặp
        ScheduledFuture<?> oldStart = activeStartTasks.remove(auctionId);
        if (oldStart != null) {
            oldStart.cancel(false);
        }

        long delayMs = Duration.between(LocalDateTime.now(), startTime).toMillis();

        if (delayMs <= 0) {
            Thread.ofVirtual()
                    .name("activate-vt-instant-" + auctionId)
                    .start(() -> activateAuction(auctionId));
        } else {
            ScheduledFuture<?> future =
                    executor.schedule(
                            () -> {
                                activeStartTasks.remove(auctionId);
                                Thread.ofVirtual()
                                        .name("activate-vt-" + auctionId)
                                        .start(() -> activateAuction(auctionId));
                            },
                            delayMs,
                            TimeUnit.MILLISECONDS);
            activeStartTasks.put(auctionId, future);
        }
    }

    /**
     * Tác vụ kích hoạt đấu giá (PENDING → ACTIVE) trên Virtual Thread.
     *
     * <p>Sử dụng khóa bi quan FOR UPDATE để kiểm tra trạng thái nguyên tử. Toàn bộ logic trigger
     * sau commit (lập lịch close, broadcast WebSocket) nằm trong khối try để đảm bảo không có JVM
     * timer ảo khi DB thất bại.
     */
    private void activateAuction(Integer auctionId) {
        Auction auction = null;
        try {
            DBConnection.beginTransaction();
            Connection conn = DBConnection.getConnection();

            // Lớp kiểm tra: khóa bi quan và xác minh trạng thái PENDING
            auction = auctionDao.getAuctionDetailsForUpdate(conn, auctionId);
            if (auction == null || auction.getStatus() != Auction.Status.PENDING) {
                DBConnection.rollbackTransaction();
                throw new AuctionActivationException(
                        "Không thể kích hoạt mở phiên. Auction phải ở trạng thái PENDING"
                                + " (Auction ID: "
                                + auctionId
                                + ")");
            }

            // Chuyển đổi trạng thái sang ACTIVE
            auction.setStatus(Auction.Status.ACTIVE);
            boolean updated = auctionDao.updateAuctionInformation(conn, auction);
            if (!updated) {
                throw new AuctionActivationException(
                        "Không thể cập nhật trạng thái sang ACTIVE trong DB"
                                + " (Auction ID: "
                                + auctionId
                                + ")");
            }

            DBConnection.commitTransaction();
            System.out.println(
                    "[AuctionSettlementScheduler] Auction ID " + auctionId + " is now ACTIVE.");

            // Sau commit thành công — kích hoạt close timer và broadcast WebSocket
            if (auction.getStatus() == Auction.Status.ACTIVE) {
                scheduleAuctionClose(auction.getId(), auction.getEndTime());

                AuctionDetailsDto detailsDto = new AuctionDetailsDto();
                detailsDto.setAuction(auction);
                BroadcastBroker.getInstance()
                        .broadcastToRoom(0, EventType.AUCTION_ACTIVATED, detailsDto, null);
            }
        } catch (Exception e) {
            DBConnection.rollbackTransaction();
            System.err.println(
                    "[AuctionSettlementScheduler] Activate Auction Failed"
                            + " for Auction #"
                            + auctionId
                            + ": "
                            + e.getMessage());
        } finally {
            DBConnection.closeConnection();
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // ĐÓNG PHIÊN: ACTIVE → ENDED/CANCELLED
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Lập lịch đóng phiên đấu giá.
     *
     * <p>Giải phóng luồng lập lịch lập tức bằng cách khởi chạy settleAuction trên Virtual Thread.
     */
    public synchronized void scheduleAuctionClose(Integer auctionId, LocalDateTime endTime) {
        long delayMs = Duration.between(LocalDateTime.now(), endTime).toMillis();
        cancelScheduledClose(auctionId);

        if (delayMs <= 0) {
            Thread.ofVirtual()
                    .name("settle-vt-instant-" + auctionId)
                    .start(() -> settleAuction(auctionId));
        } else {
            ScheduledFuture<?> future =
                    executor.schedule(
                            () -> {
                                activeCloseTasks.remove(auctionId);
                                Thread.ofVirtual()
                                        .name("settle-vt-" + auctionId)
                                        .start(() -> settleAuction(auctionId));
                            },
                            delayMs,
                            TimeUnit.MILLISECONDS);
            activeCloseTasks.put(auctionId, future);
        }
    }

    public synchronized void rescheduleAuctionClose(Integer auctionId, LocalDateTime newEndTime) {
        scheduleAuctionClose(auctionId, newEndTime);
    }

    public synchronized void cancelScheduledClose(Integer auctionId) {
        ScheduledFuture<?> future = activeCloseTasks.remove(auctionId);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * Hủy bỏ mọi tác vụ (cả start và close) đang lập lịch của auctionId chỉ định.
     *
     * <p>Được gọi khi Auction bị xóa hoặc hủy để dọn sạch JVM timer tránh lãng phí tài nguyên và
     * lỗi logic nghiệp vụ.
     */
    public synchronized void cancelScheduledTasks(Integer auctionId) {
        ScheduledFuture<?> startFuture = activeStartTasks.remove(auctionId);
        if (startFuture != null) {
            startFuture.cancel(false);
        }
        ScheduledFuture<?> closeFuture = activeCloseTasks.remove(auctionId);
        if (closeFuture != null) {
            closeFuture.cancel(false);
        }
        System.out.println(
                "[AuctionSettlementScheduler] Cancelled all JVM timers for Auction ID: "
                        + auctionId);
    }

    /**
     * PHA 1: Supreme Transaction — Kết toán nguyên tử chốt phiên với khóa bi quan FOR UPDATE.
     *
     * <p>Toàn bộ đọc/ghi dùng chung Connection duy nhất để tránh Deadlock vật lý.
     */
    private void settleAuction(Integer auctionId) {
        List<Integer> loserIds = new ArrayList<>();
        double depositAmount = 0.0;
        Bid highestBid = null;

        try {
            DBConnection.beginTransaction();
            Connection conn = DBConnection.getConnection();

            // Lớp kiểm tra: khóa bi quan và xác minh trạng thái ACTIVE
            Auction auction = auctionDao.getAuctionDetailsForUpdate(conn, auctionId);
            if (auction == null || auction.getStatus() != Auction.Status.ACTIVE) {
                DBConnection.rollbackTransaction();
                throw new AuctionSettlementException(
                        "Không thể kết toán đóng phiên. Auction phải ở trạng thái ACTIVE"
                                + " (Auction ID: "
                                + auctionId
                                + ")");
            }

            // Tái kiểm tra Snipe Extension — đề phòng gia hạn trong giây cuối trước khi khóa
            if (auction.getEndTime().isAfter(LocalDateTime.now())) {
                rescheduleAuctionClose(auctionId, auction.getEndTime());
                DBConnection.rollbackTransaction();
                return;
            }

            // Đọc danh sách thầu trên conn chung: tránh Phantom Read với Late Bids
            List<Bid> bids = bidDao.findByAuctionId(conn, auctionId);
            highestBid =
                    bids.stream()
                            .max((b1, b2) -> Double.compare(b1.getAmount(), b2.getAmount()))
                            .orElse(null);

            depositAmount = auction.getStartingPrice() * 0.3;

            if (highestBid != null) {
                Integer winnerId = highestBid.getBidderId();

                // Truyền conn vào getSellerId để dùng chung Transaction
                Integer sellerId = auctionDao.getSellerId(conn, auctionId);

                if (sellerId != null) {
                    // Phòng Deadlock: khóa dòng User theo thứ tự ID tăng dần
                    List<Integer> sortedIds =
                            Stream.of(winnerId, sellerId).sorted().collect(Collectors.toList());

                    User winner = null;
                    User seller = null;
                    for (Integer userId : sortedIds) {
                        User u = userDao.findByIdForUpdate(conn, userId);
                        if (userId.equals(winnerId)) {
                            winner = u;
                        }
                        if (userId.equals(sellerId)) {
                            seller = u;
                        }
                    }

                    if (winner != null && seller != null) {
                        BigDecimal totalWinAmount = BigDecimal.valueOf(highestBid.getAmount());
                        BigDecimal depositAmountBD = BigDecimal.valueOf(depositAmount);
                        BigDecimal remainingAmount = totalWinAmount.subtract(depositAmountBD);

                        if (winner.getBalance().compareTo(remainingAmount) >= 0) {
                            // Winner đủ tiền: thanh toán phần còn lại, chuyển full cho Seller
                            winner.commitBid(depositAmountBD);
                            winner.withdraw(remainingAmount);
                            seller.deposit(totalWinAmount);

                            userDao.updateBalanceAndLockedBalance(conn, winner);
                            userDao.updateBalanceAndLockedBalance(conn, seller);

                            userDao.insertTransactionRecord(
                                    conn,
                                    winnerId,
                                    "WITHDRAW",
                                    totalWinAmount,
                                    "SUCCESS",
                                    "AUCTION_" + auctionId);

                            userDao.insertTransactionRecord(
                                    conn,
                                    sellerId,
                                    "DEPOSIT",
                                    totalWinAmount,
                                    "SUCCESS",
                                    "AUCTION_" + auctionId);

                            auction.setWinnerId(winnerId);
                            auction.setFinalPrice(highestBid.getAmount());
                            auction.setStatus(Auction.Status.ENDED);
                        } else {
                            // Winner bùng tiền: tước cọc 30% sang Seller làm phạt
                            winner.commitBid(depositAmountBD);
                            seller.deposit(depositAmountBD);
                            userDao.updateBalanceAndLockedBalance(conn, winner);
                            userDao.updateBalanceAndLockedBalance(conn, seller);

                            userDao.insertTransactionRecord(
                                    conn,
                                    winnerId,
                                    "WITHDRAW",
                                    depositAmountBD,
                                    "SUCCESS",
                                    "PENALTY_" + auctionId);

                            userDao.insertTransactionRecord(
                                    conn,
                                    sellerId,
                                    "DEPOSIT",
                                    depositAmountBD,
                                    "SUCCESS",
                                    "PENALTY_" + auctionId);

                            auction.setWinnerId(null);
                            auction.setFinalPrice(0.0);
                            auction.setStatus(Auction.Status.CANCELLED);
                        }
                    }
                }

                // Thu thập losers để hoàn cọc ở Pha 2
                final Integer winId = highestBid.getBidderId();
                loserIds =
                        bids.stream()
                                .map(Bid::getBidderId)
                                .distinct()
                                .filter(id -> !id.equals(winId))
                                .collect(Collectors.toList());
            } else {
                // Không có ai thầu: hủy phiên đấu giá
                auction.setWinnerId(null);
                auction.setFinalPrice(0.0);
                auction.setStatus(Auction.Status.CANCELLED);
            }

            // Ghi nhận trạng thái cuối cùng xuống DB trước khi commit transaction
            auctionDao.updateAuctionInformation(conn, auction);

            DBConnection.commitTransaction();

            // Gửi thông tin chốt qua socket (tới room chi tiết và room Lobby 0)
            BroadcastBroker.getInstance()
                    .broadcastToRoom(auctionId, EventType.AUCTION_CLOSED, auction, null);
            BroadcastBroker.getInstance()
                    .broadcastToRoom(0, EventType.AUCTION_CLOSED, auction, null);

            // Gửi balance update cho winner và seller hậu commit
            if (highestBid != null) {
                try {
                    User wFresh = userDao.findById(highestBid.getBidderId());
                    if (wFresh != null) {
                        BroadcastBroker.getInstance()
                                .sendToUser(
                                        highestBid.getBidderId(),
                                        EventType.BALANCE_UPDATE,
                                        Map.of(
                                                "balance", wFresh.getBalance(),
                                                "lockedBalance", wFresh.getLockedBalance()));
                    }
                    Integer sId = auctionDao.getSellerId(null, auctionId);
                    if (sId != null) {
                        User sFresh = userDao.findById(sId);
                        if (sFresh != null) {
                            BroadcastBroker.getInstance()
                                    .sendToUser(
                                            sId,
                                            EventType.BALANCE_UPDATE,
                                            Map.of(
                                                    "balance", sFresh.getBalance(),
                                                    "lockedBalance", sFresh.getLockedBalance()));
                        }
                    }
                } catch (Exception ignored) {
                }
            }

        } catch (Exception e) {
            DBConnection.rollbackTransaction();
            System.err.println(
                    "[AuctionSettlementScheduler] Settle FAILED for Auction #"
                            + auctionId
                            + ": "
                            + e.getMessage());
            return;
        } finally {
            DBConnection.closeConnection();
        }

        // PHA 2: Hoàn cọc bất đồng bộ theo lô cho Losers (ngoài Transaction chính)
        if (!loserIds.isEmpty() && depositAmount > 0.0) {
            final double refundAmt = depositAmount;
            final List<Integer> finalLosers = loserIds;
            CompletableFuture.runAsync(
                    () -> executeBatchRefunds(auctionId, finalLosers, refundAmt));
        }
    }

    private void executeBatchRefunds(
            Integer auctionId, List<Integer> loserIds, double refundAmount) {
        int batchSize = 100;
        for (int i = 0; i < loserIds.size(); i += batchSize) {
            List<Integer> batch = loserIds.subList(i, Math.min(i + batchSize, loserIds.size()));
            executeSingleRefundBatch(auctionId, batch, refundAmount);
        }
    }

    private void executeSingleRefundBatch(
            Integer auctionId, List<Integer> batchUserIds, double refundAmount) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            System.err.println("[Scheduler] Failed to open connection for refund batch.");
            return;
        }

        try {
            // Tắt autoCommit chỉ 1 lần duy nhất cho toàn bộ Connection
            conn.setAutoCommit(false);

            List<Integer> sortedBatch = batchUserIds.stream().sorted().collect(Collectors.toList());
            for (Integer userId : sortedBatch) {
                try {
                    User u = userDao.findByIdForUpdate(conn, userId);
                    if (u != null) {
                        BigDecimal refundBD = BigDecimal.valueOf(refundAmount);
                        u.refundBalance(refundBD);
                        userDao.updateBalanceAndLockedBalance(conn, u);

                        userDao.insertTransactionRecord(
                                conn,
                                userId,
                                "DEPOSIT",
                                refundBD,
                                "SUCCESS",
                                "BID_REFUND_" + auctionId);
                    }
                    conn.commit();

                    // Gửi balance update cho từng user sau khi commit thành công
                    User uFresh = userDao.findById(userId);
                    if (uFresh != null) {
                        BroadcastBroker.getInstance()
                                .sendToUser(
                                        userId,
                                        EventType.BALANCE_UPDATE,
                                        Map.of(
                                                "balance", uFresh.getBalance(),
                                                "lockedBalance", uFresh.getLockedBalance()));
                    }
                } catch (Exception e) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        System.err.println("[Scheduler] Rollback failed: " + ex.getMessage());
                    }
                    System.err.println(
                            "[Scheduler] Refund FAILED for user ID "
                                    + userId
                                    + ": "
                                    + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.err.println("[Scheduler] Failed to configure autoCommit: " + e.getMessage());
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
            DBConnection.closeConnection();
        }
    }
}
