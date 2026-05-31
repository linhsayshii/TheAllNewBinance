package com.auction.server.controller;

import com.auction.core.dto.bid.GetBidByAuctionIdRequest;
import com.auction.core.dto.bid.GetBidByBidderIdRequest;
import com.auction.core.dto.bid.PlaceBid;
import com.auction.core.services.IBidService;
import java.util.concurrent.CompletableFuture;

public final class BidController extends BaseController {
    private final IBidService bidService;

    public BidController(IBidService bidService) {
        this.bidService = bidService;
    }

    /** Full async — returns CompletableFuture instead of blocking with .join() */
    public CompletableFuture<String> placeBid(String request) {
        return handleAsync(
                request,
                PlaceBid.class,
                req -> bidService.placeBid(req).thenApply(bid -> (Object) bid),
                "Failed to place bid");
    }

    public String getBidsByAuctionId(String request) {
        return handleSync(
                request,
                GetBidByAuctionIdRequest.class,
                req -> bidService.getBidsByAuctionId(req).join(),
                "Internal server error");
    }

    public String getBidsByBidderId(String request) {
        return handleSync(
                request,
                GetBidByBidderIdRequest.class,
                req -> bidService.getBidsByBidderId(req).join(),
                "Internal server error");
    }
}
