package com.auction.server.controller;

import com.auction.core.dto.bid.GetBidByAuctionIdRequest;
import com.auction.core.dto.bid.GetBidByBidderIdRequest;
import com.auction.core.dto.bid.PlaceBid;
import com.auction.core.services.IBidService;
import com.auction.core.utils.JsonMapper;
import java.util.concurrent.CompletableFuture;

public class BidController extends BaseController {
    private final IBidService bidService;

    public BidController(IBidService bidService) {
        this.bidService = bidService;
    }

    /** Full async — returns CompletableFuture instead of blocking with .join() */
    public CompletableFuture<String> placeBid(String request) {
        if (request == null) {
            return CompletableFuture.completedFuture(
                    ApiResponse.error("Request payload is required"));
        }
        try {
            PlaceBid placeBidRequest = JsonMapper.fromJson(request, PlaceBid.class);
            return bidService
                    .placeBid(placeBidRequest)
                    .thenApply(
                            bid -> {
                                if (bid == null) {
                                    return ApiResponse.error("Failed to place bid");
                                }
                                return ApiResponse.success(bid);
                            })
                    .exceptionally(
                            ex -> {
                                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                                return ApiResponse.error(cause.getMessage());
                            });
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return CompletableFuture.completedFuture(ApiResponse.error(ex.getMessage()));
        } catch (Exception ex) {
            return CompletableFuture.completedFuture(
                    ApiResponse.error("Internal server error while placing bid"));
        }
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
