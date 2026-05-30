package com.auction.server.services;

import com.auction.core.auction.Bid;
import com.auction.core.dto.bid.PlaceBid;
import java.util.concurrent.CompletableFuture;

/**
 * Wraps a validated bid request with its result future. Submitted to BidQueueManager for serial
 * processing per auction.
 */
public class BidTask {
    private final PlaceBid request;
    private final CompletableFuture<Bid> resultFuture;

    // Pre-validated data carried from Pha 1 so the consumer doesn't re-query
    private final com.auction.core.auction.Auction snapshot;
    private final boolean firstBid;

    public BidTask(
            PlaceBid request,
            CompletableFuture<Bid> resultFuture,
            com.auction.core.auction.Auction snapshot,
            boolean firstBid) {
        this.request = request;
        this.resultFuture = resultFuture;
        this.snapshot = snapshot;
        this.firstBid = firstBid;
    }

    public PlaceBid getRequest() {
        return request;
    }

    public CompletableFuture<Bid> getResultFuture() {
        return resultFuture;
    }

    public com.auction.core.auction.Auction getSnapshot() {
        return snapshot;
    }

    public boolean isFirstBid() {
        return firstBid;
    }
}
