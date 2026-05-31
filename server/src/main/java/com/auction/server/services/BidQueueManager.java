package com.auction.server.services;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.exception.auction.AuctionClosedException;
import com.auction.core.exception.auction.InvalidBidException;
import com.auction.core.exception.user.UserNotFoundException;
import com.auction.core.exception.wallet.InsufficientBalanceException;
import com.auction.core.protocol.EventType;
import com.auction.core.services.IAuctionService;
import com.auction.core.users.User;
import com.auction.server.dao.DBConnection;
import com.auction.server.dao.impl.IAuctionDao;
import com.auction.server.dao.impl.IBidDao;
import com.auction.server.dao.impl.IUserDao;
import com.auction.server.network.BroadcastBroker;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Manages per-auction bid queues to serialize concurrent bids at the application layer instead of
 * relying on database row locks (SELECT ... FOR UPDATE).
 *
 * <p>Each auction gets its own LinkedBlockingQueue with a dedicated consumer thread. Consumers are
 * spawned on-demand and exit when the queue is drained.
 */
public class BidQueueManager {
    private static final int QUEUE_TIMEOUT_SECONDS = 5;
    private static final int CONSUMER_IDLE_SECONDS = 3;

    private final ConcurrentHashMap<Integer, LinkedBlockingQueue<BidTask>> auctionQueues =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Boolean> activeConsumers = new ConcurrentHashMap<>();
    private final ExecutorService consumerPool =
            Executors.newCachedThreadPool(
                    r -> {
                        Thread t = new Thread(r, "bid-consumer");
                        t.setDaemon(true);
                        return t;
                    });

    private final IBidDao bidDao;
    private final IAuctionService auctionService;
    private final IAuctionDao auctionDao;
    private final IUserDao userDao;

    private volatile boolean shuttingDown = false;

    public BidQueueManager(
            IBidDao bidDao,
            IAuctionService auctionService,
            IAuctionDao auctionDao,
            IUserDao userDao) {
        this.bidDao = bidDao;
        this.auctionService = auctionService;
        this.auctionDao = auctionDao;
        this.userDao = userDao;
    }

    /**
     * Submit a pre-validated bid task into the per-auction queue. Returns the CompletableFuture
     * that will be completed by the consumer.
     */
    public CompletableFuture<Bid> submitBid(BidTask task) {
        if (shuttingDown) {
            task.getResultFuture()
                    .completeExceptionally(
                            new IllegalStateException(
                                    "Server is shutting down, cannot accept bids"));
            return task.getResultFuture();
        }

        int auctionId = task.getRequest().getAuctionId();
        LinkedBlockingQueue<BidTask> queue =
                auctionQueues.computeIfAbsent(auctionId, k -> new LinkedBlockingQueue<>());

        boolean offered = queue.offer(task);
        if (!offered) {
            task.getResultFuture()
                    .completeExceptionally(
                            new IllegalStateException("Bid queue is full, please retry"));
            return task.getResultFuture();
        }

        ensureConsumerRunning(auctionId, queue);
        return task.getResultFuture();
    }

    /** Spawn a consumer thread for this auction if one isn't already running. */
    private void ensureConsumerRunning(int auctionId, LinkedBlockingQueue<BidTask> queue) {
        activeConsumers.computeIfAbsent(
                auctionId,
                k -> {
                    consumerPool.submit(() -> consumeLoop(auctionId, queue));
                    return Boolean.TRUE;
                });
    }

    /**
     * Consumer loop: polls tasks from the queue and processes them serially. Exits when the queue
     * is idle for CONSUMER_IDLE_SECONDS.
     */
    private void consumeLoop(int auctionId, LinkedBlockingQueue<BidTask> queue) {
        try {
            while (!shuttingDown) {
                BidTask task = queue.poll(CONSUMER_IDLE_SECONDS, TimeUnit.SECONDS);
                if (task == null) {
                    // Queue idle — exit consumer, allow re-spawn if new bids arrive
                    break;
                }
                processBidTask(task);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // Mark consumer as inactive so a new one can be spawned
            activeConsumers.remove(auctionId);

            // Edge case: a task was added between poll timeout and remove.
            // Re-check and re-spawn if needed.
            if (!queue.isEmpty() && !shuttingDown) {
                ensureConsumerRunning(auctionId, queue);
            }
        }
    }

    /**
     * Process a single bid task: validate price against DB, update auction, save bid. All within a
     * DB transaction using shared Connection to prevent Deadlock. The result (success or error)
     * completes the task's future.
     */
    private void processBidTask(BidTask task) {
        CompletableFuture<Bid> future = task.getResultFuture();
        boolean triggerReschedule = false;
        int rescheduleAuctionId = -1;
        LocalDateTime rescheduleEndTime = null;

        try {
            DBConnection.beginTransaction();
            Connection conn = DBConnection.getConnection();

            int auctionId = task.getRequest().getAuctionId();
            int bidderId = task.getRequest().getBidderId();

            // 1. Facade đồng bộ: Khóa bi quan dòng Auction, giữ nguyên Thread Context
            Auction auction = auctionService.getAuctionDetailsForUpdate(conn, auctionId);
            if (auction == null || auction.getStatus() != Auction.Status.ACTIVE) {
                throw new AuctionClosedException(
                        "Phiên đấu giá đã kết thúc hoặc không ở trạng thái ACTIVE.");
            }

            // 2. Khóa bi quan dòng User, tái kiểm tra số dư thực tế
            User user = userDao.findByIdForUpdate(conn, bidderId);
            if (user == null) {
                throw new UserNotFoundException("Không tìm thấy người dùng thầu.");
            }

            // 3. hasBid trong Transaction để tránh race condition đặt cọc
            boolean hasBidBefore = bidDao.hasBid(conn, auctionId, bidderId);
            double amount = task.getRequest().getAmount();

            if (!hasBidBefore) {
                double depositAmount = auction.getStartingPrice() * 0.3;
                BigDecimal depositBD = BigDecimal.valueOf(depositAmount);
                if (user.getBalance().compareTo(depositBD) < 0) {
                    throw new InsufficientBalanceException("Số dư khả dụng không đủ đóng cọc 30%.");
                }
                // Dùng domain method holdBalance() — tự kiểm tra số dư và cập nhật
                // balance/lockedBalance
                user.holdBalance(depositBD);
                userDao.updateBalanceAndLockedBalance(conn, user);
                userDao.insertTransactionRecord(
                        conn,
                        bidderId,
                        "WITHDRAW",
                        depositBD,
                        "SUCCESS",
                        "BID_DEPOSIT_" + auctionId);
            }

            if (user.getBalance().compareTo(BigDecimal.valueOf(amount)) < 0) {
                throw new InsufficientBalanceException(
                        "Số dư khả dụng không đủ bảo chứng cho giá đặt mới: " + amount);
            }

            Bid bid = new Bid(null, auctionId, bidderId, amount);

            // 4. processBid với conn dùng chung — cùng một Connection vật lý
            auctionService.processBid(conn, bid, auction).join();

            // 5. Snipe Extension trong Transaction; ghi nhận cờ để reschedule Post-Commit
            boolean extended = auction.applySnipeExtension(LocalDateTime.now());
            if (extended) {
                auctionDao.updateAuctionInformation(conn, auction);
                triggerReschedule = true;
                rescheduleAuctionId = auction.getId();
                rescheduleEndTime = auction.getEndTime();
            }

            boolean saved = bidDao.saveBid(conn, bid);
            if (!saved) {
                throw new InvalidBidException("Không thể ghi nhận lịch sử thầu vào cơ sở dữ liệu.");
            }

            DBConnection.commitTransaction();
            future.complete(bid);
        } catch (Exception e) {
            DBConnection.rollbackTransaction();
            future.completeExceptionally(e);
        } finally {
            DBConnection.closeConnection();
        }

        // 6. Reschedule JVM scheduler: CHỈ chạy Post-Commit sau Transaction DB thành công
        if (triggerReschedule && rescheduleEndTime != null) {
            AuctionSettlementScheduler.getInstance()
                    .rescheduleAuctionClose(rescheduleAuctionId, rescheduleEndTime);

            // Broadcast gia hạn endTime đến toàn bộ client đang trong room
            final int broadcastAuctionId = rescheduleAuctionId;
            final LocalDateTime broadcastEndTime = rescheduleEndTime;
            BroadcastBroker.getInstance()
                    .broadcastToRoom(
                            broadcastAuctionId,
                            EventType.AUCTION_EXTENDED,
                            Map.of("auctionId", broadcastAuctionId, "newEndTime", broadcastEndTime),
                            null);
        }
    }

    /** Graceful shutdown: stop accepting new bids, drain existing queues. */
    public void shutdown() {
        shuttingDown = true;
        consumerPool.shutdown();
        try {
            if (!consumerPool.awaitTermination(10, TimeUnit.SECONDS)) {
                consumerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            consumerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Fail any remaining tasks
        auctionQueues.forEach(
                (id, queue) -> {
                    BidTask task;
                    while ((task = queue.poll()) != null) {
                        task.getResultFuture()
                                .completeExceptionally(
                                        new IllegalStateException(
                                                "Server shutdown — bid not processed"));
                    }
                });
        auctionQueues.clear();
    }
}
