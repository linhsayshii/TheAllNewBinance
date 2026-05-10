package com.auction.server.controller;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.auction.core.protocol.EventType;
import com.auction.core.utils.JsonMapper;

public class RequestDispatcher {
    private final Map<EventType, Function<String, CompletableFuture<String>>> handlers = new EnumMap<>(EventType.class);

    public RequestDispatcher(UserController userCtrl, AuctionController auctionCtrl, BidController bidCtrl) {
        registerHandlers(userCtrl, auctionCtrl, bidCtrl);
    }

    /**
     * Register all event handlers.
     * Adding a new EventType = adding one line here — OCP compliant.
     */
    private void registerHandlers(UserController userCtrl, AuctionController auctionCtrl, BidController bidCtrl) {
        // User handlers
        registerSync(EventType.LOGIN, userCtrl::login);
        registerSync(EventType.REGISTER, userCtrl::register);
        registerSync(EventType.UPDATE_PROFILE, userCtrl::updateProfile);
        registerSync(EventType.CHANGE_PASSWORD, userCtrl::changePassword);
        registerSync(EventType.LOGOUT, userCtrl::logout);

        // Auction handlers
        registerSync(EventType.CREATE_AUCTION, auctionCtrl::createAuction);
        registerSync(EventType.GET_AUCTION_DETAILS, auctionCtrl::getAuctionDetails);
        registerSync(EventType.GET_AUCTIONS_BY_SELLER, auctionCtrl::getAuctionsBySellerId);
        registerSync(EventType.GET_PUBLIC_AUCTIONS, auctionCtrl::getPublicAuctions);

        // Bid handlers
        handlers.put(EventType.PLACE_BID, bidCtrl::placeBid); // async native
        registerSync(EventType.GET_BIDS_BY_AUCTION_ID, bidCtrl::getBidsByAuctionId);
        registerSync(EventType.GET_BIDS_BY_BIDDER_ID, bidCtrl::getBidsByBidderId);
    }

    /**
     * Wraps a synchronous handler (String → String) into the async-compatible Function.
     */
    private void registerSync(EventType type, Function<String, String> syncHandler) {
        handlers.put(type, payload -> CompletableFuture.completedFuture(syncHandler.apply(payload)));
    }

    /**
     * Dispatch trả về CompletableFuture<String> để hỗ trợ full async cho PLACE_BID.
     * Các operation khác vẫn synchronous, wrap trong completedFuture.
     */
    public CompletableFuture<String> dispatch(Integer sessionUserId, String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return CompletableFuture.completedFuture(ApiResponse.error("Request body is empty"));
        }

        Object parsed;
        try {
            parsed = JsonMapper.fromJson(rawJson, Object.class);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                    ApiResponse.error("Malformed JSON Syntax: " + e.getMessage()));
        }

        if (!(parsed instanceof Map<?, ?> node)) {
            return CompletableFuture.completedFuture(ApiResponse.error("Invalid JSON format"));
        }

        Object typeNode = node.get("type");
        if (typeNode == null) {
            return CompletableFuture.completedFuture(ApiResponse.error("Missing type"));
        }

        EventType type = EventType.fromWireValue(typeNode);
        if (type == null) {
            return CompletableFuture.completedFuture(ApiResponse.error("Unknown type: " + typeNode));
        }

        Object correlationId = node.get("correlationId");

        // Cấp quyền bảo mật: Chặn đứng truy cập nặc danh và tự động đè ID
        if (sessionUserId == null) {
            if (!isAnonymousAllowed(type)) {
                return CompletableFuture.completedFuture(
                        ApiResponse.error("Unauthorized: Please login first!"));
            }
        } else {
            Object payloadObj = node.get("payload");
            if (payloadObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> p = (Map<String, Object>) payloadObj;
                overridePayloadIdentity(type, p, sessionUserId);
            }
        }

        String payload = extractPayload(node.get("payload"));

        // Route to handler via registry
        CompletableFuture<String> responseFuture;
        try {
            Function<String, CompletableFuture<String>> handler = handlers.get(type);
            if (handler == null) {
                responseFuture = CompletableFuture.completedFuture(
                        ApiResponse.error("No handler registered for: " + type));
            } else {
                responseFuture = handler.apply(payload);
            }
        } catch (Exception ex) {
            System.err.println("Unhandled exception during dispatch: " + ex.getMessage());
            ex.printStackTrace();
            responseFuture = CompletableFuture.completedFuture(ApiResponse.error("Internal Server Error"));
        }

        // Enrich response with type and correlationId
        return responseFuture.thenApply(responseRaw -> enrichResponse(responseRaw, type, correlationId));
    }

    private String enrichResponse(String responseRaw, EventType type, Object correlationId) {
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
            case UPDATE_PROFILE, CHANGE_PASSWORD, LOGOUT -> payload.put("userId", sessionUserId);
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
}
