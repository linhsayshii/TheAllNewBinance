package com.auction.server.services;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.services.IAuctionService;
import com.auction.server.dao.DBConnection;
import com.auction.server.dao.impl.IBidDao;

/**
 * Manages per-auction bid queues to serialize concurrent bids at the application layer
 * instead of relying on database row locks (SELECT ... FOR UPDATE).
 *
 * Each auction gets its own LinkedBlockingQueue with a dedicated consumer thread.
 * Consumers are spawned on-demand and exit when the queue is drained.
 */
public class BidQueueManager {
    private static final int QUEUE_TIMEOUT_SECONDS = 5;
    private static final int CONSUMER_IDLE_SECONDS = 3;

    private final ConcurrentHashMap<Integer, LinkedBlockingQueue<BidTask>> auctionQueues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Boolean> activeConsumers = new ConcurrentHashMap<>();
    private final ExecutorService consumerPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "bid-consumer");
        t.setDaemon(true);
        return t;
    });

    private final IBidDao bidDao;
    private final IAuctionService auctionService;

    private volatile boolean shuttingDown = false;

    public BidQueueManager(IBidDao bidDao, IAuctionService auctionService) {
        this.bidDao = bidDao;
        this.auctionService = auctionService;
    }

    /**
     * Submit a pre-validated bid task into the per-auction queue.
     * Returns the CompletableFuture that will be completed by the consumer.
     */
    public CompletableFuture<Bid> submitBid(BidTask task) {
        if (shuttingDown) {
            task.getResultFuture().completeExceptionally(
                new IllegalStateException("Server is shutting down, cannot accept bids"));
            return task.getResultFuture();
        }

        int auctionId = task.getRequest().getAuctionId();
        LinkedBlockingQueue<BidTask> queue = auctionQueues.computeIfAbsent(
            auctionId, k -> new LinkedBlockingQueue<>());

        boolean offered = queue.offer(task);
        if (!offered) {
            task.getResultFuture().completeExceptionally(
                new IllegalStateException("Bid queue is full, please retry"));
            return task.getResultFuture();
        }

        ensureConsumerRunning(auctionId, queue);
        return task.getResultFuture();
    }

    /**
     * Spawn a consumer thread for this auction if one isn't already running.
     */
    private void ensureConsumerRunning(int auctionId, LinkedBlockingQueue<BidTask> queue) {
        activeConsumers.computeIfAbsent(auctionId, k -> {
            consumerPool.submit(() -> consumeLoop(auctionId, queue));
            return Boolean.TRUE;
        });
    }

    /**
     * Consumer loop: polls tasks from the queue and processes them serially.
     * Exits when the queue is idle for CONSUMER_IDLE_SECONDS.
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
     * Process a single bid task: validate price against DB, update auction, save bid.
     * All within a DB transaction. The result (success or error) completes the task's future.
     */
    private void processBidTask(BidTask task) {
        CompletableFuture<Bid> future = task.getResultFuture();
        try {
            DBConnection.beginTransaction();

            Bid bid = new Bid(
                null,
                task.getRequest().getAuctionId(),
                task.getRequest().getBidderId(),
                task.getRequest().getAmount()
            );

            // Use auction snapshot for bid increment/snipe, but re-read current_price from DB
            Auction auction = task.getSnapshot();
            auctionService.processBid(bid, auction).join();

            boolean saved = bidDao.saveBid(bid);
            if (!saved) {
                throw new IllegalStateException("Cannot persist bid");
            }

            DBConnection.commitTransaction();
            future.complete(bid);
        } catch (IllegalArgumentException | IllegalStateException e) {
            DBConnection.rollbackTransaction();
            future.completeExceptionally(e);
        } catch (Exception e) {
            DBConnection.rollbackTransaction();
            future.completeExceptionally(
                new RuntimeException("Transaction error while placing bid", e));
        } finally {
            DBConnection.closeConnection();
        }
    }

    /**
     * Graceful shutdown: stop accepting new bids, drain existing queues.
     */
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
        auctionQueues.forEach((id, queue) -> {
            BidTask task;
            while ((task = queue.poll()) != null) {
                task.getResultFuture().completeExceptionally(
                    new IllegalStateException("Server shutdown — bid not processed"));
            }
        });
        auctionQueues.clear();
    }
}
