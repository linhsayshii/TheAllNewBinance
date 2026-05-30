package com.auction.server.controller;

import com.auction.core.auction.Auction;
import com.auction.core.dto.auction.CreateAuctionRequest;
import com.auction.core.dto.auction.GetAuctionBySellerIdRequest;
import com.auction.core.dto.auction.GetAuctionDetailsRequest;
import com.auction.core.dto.auction.GetFeaturedAuctionsRequest;
import com.auction.core.dto.auction.PromoteAuctionRequest;
import com.auction.core.services.IAuctionService;
import java.util.List;
import java.util.Map;

public class AuctionController extends BaseController {
    private final IAuctionService auctionService;

    public AuctionController(IAuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public String createAuction(String payload) {
        return handleSync(
                payload,
                CreateAuctionRequest.class,
                req -> auctionService.createAuction(req).join(),
                "Internal server error");
    }

    public String getAuctionDetails(String payload) {
        return handleSync(
                payload,
                GetAuctionDetailsRequest.class,
                req -> {
                    com.auction.core.dto.auction.AuctionDetailsDto details =
                            auctionService.getAuctionDetails(req.getAuctionId()).join();
                    if (details == null) {
                        throw new IllegalArgumentException("Auction not found");
                    }
                    return details;
                },
                "Auction not found");
    }

    public String getAuctionsBySellerId(String payload) {
        return handleSync(
                payload,
                GetAuctionBySellerIdRequest.class,
                req -> {
                    List<Auction> auctions =
                            auctionService.getAuctionsBySellerId(req).join();
                    if (auctions == null) {
                        throw new IllegalArgumentException("Failed to get auctions by seller id");
                    }
                    return auctions;
                },
                "Internal server error");
    }

    public String getPublicAuctions(String payload) {
        String requestPayload = (payload == null || payload.isBlank() || "null".equals(payload)) ? "{}" : payload;
        return handleSync(
                requestPayload,
                com.auction.core.dto.auction.GetPublicAuctionsRequest.class,
                req -> auctionService.getPublicAuctions(req).join(),
                "Internal server error");
    }

    public String promoteAuction(String payload) {
        return handleSync(
                payload,
                PromoteAuctionRequest.class,
                req -> {
                    Boolean success = auctionService.promoteAuction(req).join();
                    if (!Boolean.TRUE.equals(success)) {
                        throw new IllegalStateException("Promote failed, please try again.");
                    }
                    return "Promoted successfully!";
                },
                "Promote failed, please try again.");
    }

    public String getFeaturedAuctions(String payload) {
        String requestPayload = (payload == null || payload.isBlank() || "null".equals(payload)) ? "{}" : payload;
        return handleSync(
                requestPayload,
                GetFeaturedAuctionsRequest.class,
                req -> auctionService.getFeaturedAuctions(req).join(),
                "Internal server error");
    }

    public String getAllAuctionsForAdmin(String payload) {
        String requestPayload = (payload == null || payload.isBlank() || "null".equals(payload)) ? "{}" : payload;
        return handleSync(
                requestPayload,
                Map.class,
                req -> {
                    String status = (String) req.getOrDefault("status", null);
                    int page = req.containsKey("page") ? ((Number) req.get("page")).intValue() : 1;
                    int size = req.containsKey("size") ? ((Number) req.get("size")).intValue() : 20;
                    return auctionService.getAllAuctionsForAdmin(status, page, size).join();
                },
                "Internal server error");
    }
}
