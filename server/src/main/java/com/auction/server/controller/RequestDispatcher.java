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

    public String dispatch(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return error("Request body is empty");
        }

        Object parsed = JsonMapper.fromJson(rawJson, Object.class);
        if (!(parsed instanceof Map<?, ?> node)) {
            return error("Invalid JSON format");
        }

        Object typeNode = node.get("type");
        if (typeNode == null) {
            return error("Missing type");
        }

        String type = String.valueOf(typeNode);
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
            case "GET_AUCTION" -> auctionCtrl == null
                    ? error("Auction controller is not configured")
                    : auctionCtrl.getAuction(payload);
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
