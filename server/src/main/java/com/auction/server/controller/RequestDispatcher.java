package com.auction.server.controller;

import java.util.HashMap;
import java.util.Map;

import com.auction.core.utils.JsonMapper;

public class RequestDispatcher {
    private final UserController userCtrl;
    private final AuctionController auctionCtrl;
    private final BidController bidCtrl;

    public RequestDispatcher(UserController userCtrl, AuctionController auctionCtrl, BidController bidCtrl) {
        this.userCtrl = userCtrl;
        this.auctionCtrl = auctionCtrl;
        this.bidCtrl = bidCtrl;
    }

    public String dispatch(Integer sessionUserId, String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return error("Request body is empty");
        }

        Object parsed;
        try {
            parsed = JsonMapper.fromJson(rawJson, Object.class);
        } catch (Exception e) {
            return error("Malformed JSON Syntax: " + e.getMessage());
        }

        if (!(parsed instanceof Map<?, ?> node)) {
            return error("Invalid JSON format");
        }

        Object typeNode = node.get("type");
        if (typeNode == null) {
            return error("Missing type");
        }

        String type = String.valueOf(typeNode);

        // Cấp quyền bảo mật: Chặn đứng truy cập nặc danh và tự động đè ID
        if (sessionUserId == null) {
            if (!type.equals("LOGIN") && !type.equals("REGISTER") && !type.equals("GET_AUCTION_DETAILS")
                    && !type.equals("GET_BIDS_BY_AUCTION_ID")) {
                return error("Unauthorized: Please login first!");
            }
        } else {
            Object payloadObj = node.get("payload");
            if (payloadObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> p = (Map<String, Object>) payloadObj;
                // Sang chấn áp đè ID thật từ phiên Socket
                if (type.equals("PLACE_BID") || type.equals("GET_BIDS_BY_BIDDER_ID")) {
                    p.put("bidderId", sessionUserId);
                } else if (type.equals("UPDATE_PROFILE") || type.equals("CHANGE_PASSWORD")) {
                    p.put("userId", sessionUserId);
                } else if (type.equals("CREATE_AUCTION") || type.equals("GET_AUCTIONS_BY_SELLER")) {
                    p.put("sellerId", sessionUserId);
                }
            }
        }

        String payload = extractPayload(node.get("payload"));

        return switch (type) {
            case "LOGIN" -> userCtrl == null
                    ? error("User controller is not configured")
                    : userCtrl.login(payload);
            case "REGISTER" -> userCtrl == null
                    ? error("User controller is not configured")
                    : userCtrl.register(payload);
            case "UPDATE_PROFILE" -> userCtrl == null
                    ? error("User controller is not configured")
                    : userCtrl.updateProfile(payload);
            case "CHANGE_PASSWORD" -> userCtrl == null
                    ? error("User controller is not configured")
                    : userCtrl.changePassword(payload);
            case "PLACE_BID" -> bidCtrl == null
                    ? error("Bid controller is not configured")
                    : bidCtrl.placeBid(payload);
            case "CREATE_AUCTION" -> auctionCtrl == null
                    ? error("Auction controller is not configured")
                    : auctionCtrl.createAuction(payload);
            case "GET_AUCTION_DETAILS" -> auctionCtrl == null
                    ? error("Auction controller is not configured")
                    : auctionCtrl.getAuctionDetails(payload);
            case "GET_AUCTIONS_BY_SELLER" -> auctionCtrl == null
                    ? error("Auction controller is not configured")
                    : auctionCtrl.getAuctionsBySellerId(payload);
            case "GET_BIDS_BY_AUCTION_ID" -> bidCtrl == null
                    ? error("Bid controller is not configured")
                    : bidCtrl.getBidsByAuctionId(payload);
            case "GET_BIDS_BY_BIDDER_ID" -> bidCtrl == null
                    ? error("Bid controller is not configured")
                    : bidCtrl.getBidsByBidderId(payload);
            default -> error("Unknown type: " + type);
        };
    }

    private String extractPayload(Object payloadNode) {
        if (payloadNode == null) {
            return null;
        }
        return JsonMapper.toJson(payloadNode);
    }

    private String error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return JsonMapper.toJson(response);
    }
}
