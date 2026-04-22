package com.auction.server.controller;

import java.util.HashMap;
import java.util.Map;

import com.auction.core.protocol.EventType;
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

        EventType type = EventType.fromWireValue(typeNode);
        if (type == null) {
            return error("Unknown type: " + typeNode);
        }

        Object correlationId = node.get("correlationId");

        // Cấp quyền bảo mật: Chặn đứng truy cập nặc danh và tự động đè ID
        if (sessionUserId == null) {
            if (!isAnonymousAllowed(type)) {
                return error("Unauthorized: Please login first!");
            }
        } else {
            Object payloadObj = node.get("payload");
            if (payloadObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> p = (Map<String, Object>) payloadObj;
                // Sang chấn áp đè ID thật từ phiên Socket
                overridePayloadIdentity(type, p, sessionUserId);
            }
        }

        String payload = extractPayload(node.get("payload"));

        String responseRaw = switch (type) {
            case EventType.LOGIN -> userCtrl == null
                    ? error("User controller is not configured")
                    : userCtrl.login(payload);
            case EventType.REGISTER -> userCtrl == null
                    ? error("User controller is not configured")
                    : userCtrl.register(payload);
            case EventType.UPDATE_PROFILE -> userCtrl == null
                    ? error("User controller is not configured")
                    : userCtrl.updateProfile(payload);
            case EventType.CHANGE_PASSWORD -> userCtrl == null
                    ? error("User controller is not configured")
                    : userCtrl.changePassword(payload);
            case EventType.PLACE_BID -> bidCtrl == null
                    ? error("Bid controller is not configured")
                    : bidCtrl.placeBid(payload);
            case EventType.CREATE_AUCTION -> auctionCtrl == null
                    ? error("Auction controller is not configured")
                    : auctionCtrl.createAuction(payload);
            case EventType.GET_AUCTION_DETAILS -> auctionCtrl == null
                    ? error("Auction controller is not configured")
                    : auctionCtrl.getAuctionDetails(payload);
            case EventType.GET_AUCTIONS_BY_SELLER -> auctionCtrl == null
                    ? error("Auction controller is not configured")
                    : auctionCtrl.getAuctionsBySellerId(payload);
            case EventType.GET_BIDS_BY_AUCTION_ID -> bidCtrl == null
                    ? error("Bid controller is not configured")
                    : bidCtrl.getBidsByAuctionId(payload);
            case EventType.GET_BIDS_BY_BIDDER_ID -> bidCtrl == null
                    ? error("Bid controller is not configured")
                    : bidCtrl.getBidsByBidderId(payload);
            case EventType.GET_PUBLIC_AUCTIONS -> auctionCtrl == null
                    ? error("Auction controller is not configured")
                    : auctionCtrl.getPublicAuctions(payload);
        };
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = JsonMapper.fromJson(responseRaw, Map.class);
            responseMap.put("type", type.wireValue());
            if (correlationId != null) {
                responseMap.put("correlationId", correlationId);
            }
            return JsonMapper.toJson(responseMap);
        } catch (Exception e) {
            return responseRaw;
        }
    }

    private boolean isAnonymousAllowed(EventType type) {
        return switch (type) {
            case LOGIN, REGISTER, GET_AUCTION_DETAILS, GET_BIDS_BY_AUCTION_ID, GET_PUBLIC_AUCTIONS -> true;
            default -> false;
        };
    }

    private void overridePayloadIdentity(EventType type, Map<String, Object> payload, Integer sessionUserId) {
        switch (type) {
            case PLACE_BID, GET_BIDS_BY_BIDDER_ID -> payload.put("bidderId", sessionUserId);
            case UPDATE_PROFILE, CHANGE_PASSWORD -> payload.put("userId", sessionUserId);
            case CREATE_AUCTION, GET_AUCTIONS_BY_SELLER -> payload.put("sellerId", sessionUserId);
            default -> {
                // No identity override required for this event type.
            }
        }
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
