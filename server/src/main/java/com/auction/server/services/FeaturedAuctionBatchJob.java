package com.auction.server.services;

import com.auction.server.dao.impl.IAuctionDao;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Batch Job tự động reset isFeatured = false cho các Star Auction đã hết hạn. Chạy hàng ngày vào
 * đúng 00:00 (nửa đêm). Được khởi động từ ServerApp và shutdown gracefully khi server dừng.
 */
public class FeaturedAuctionBatchJob {
    private final IAuctionDao auctionDao;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(
                    r -> {
                        Thread t = new Thread(r, "featured-auction-batch-job");
                        t.setDaemon(true); // Không block JVM shutdown
                        return t;
                    });

    public FeaturedAuctionBatchJob(IAuctionDao auctionDao) {
        this.auctionDao = auctionDao;
    }

    /** Khởi động job. Lần đầu chạy đúng vào 00:00 ngày hôm sau, sau đó lặp mỗi 24 tiếng. */
    public void start() {
        long initialDelay = computeInitialDelaySeconds();
        System.out.printf(
                "[FeaturedAuctionBatchJob] Started. First run in %d seconds (at midnight).%n",
                initialDelay);
        scheduler.scheduleAtFixedRate(
                this::run, initialDelay, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
    }

    private void run() {
        try {
            System.out.println("[FeaturedAuctionBatchJob] Running at " + LocalDateTime.now());
            int count = auctionDao.resetExpiredFeaturedAuctions();
            System.out.printf(
                    "[FeaturedAuctionBatchJob] Done. Reset %d expired featured auctions.%n", count);
        } catch (Exception e) {
            System.err.println("[FeaturedAuctionBatchJob] Error during run: " + e.getMessage());
        }
    }

    public void stop() {
        scheduler.shutdownNow();
        System.out.println("[FeaturedAuctionBatchJob] Stopped.");
    }

    /** Tính số giây còn lại đến 00:00 ngày hôm sau. */
    private long computeInitialDelaySeconds() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextMidnight = LocalDate.now().plusDays(1).atTime(LocalTime.MIDNIGHT);
        return Duration.between(now, nextMidnight).getSeconds();
    }
}
