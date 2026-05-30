package com.auction.server.controller;

import com.auction.core.protocol.EventType;
import com.auction.core.utils.JsonMapper;
import com.auction.server.dao.impl.IUserDao;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RequestDispatcher {
    private final UserController userCtrl;
    private final AuctionController auctionCtrl;
    private final BidController bidCtrl;
    private final ItemController itemCtrl;
    private final IUserDao userDao; // dùng để xác minh quyền Admin phía Server

    public RequestDispatcher(
            UserController userCtrl,
            AuctionController auctionCtrl,
            BidController bidCtrl,
            ItemController itemCtrl,
            IUserDao userDao) {
        this.userCtrl = userCtrl;
        this.auctionCtrl = auctionCtrl;
        this.bidCtrl = bidCtrl;
        this.itemCtrl = itemCtrl;
        this.userDao = userDao;
    }

    /**
     * Dispatch trả về CompletableFuture<String> để hỗ trợ full async cho PLACE_BID.
     * Các operation
     * khác vẫn synchronous, wrap trong completedFuture.
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
            return CompletableFuture.completedFuture(
                    ApiResponse.error("Unknown type: " + typeNode));
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

        CompletableFuture<String> responseFuture;
        try {
            responseFuture = switch (type) {
                case EventType.LOGIN -> CompletableFuture.completedFuture(
                        userCtrl == null
                                ? error("User controller is not configured")
                                : userCtrl.login(payload));
                case EventType.REGISTER -> CompletableFuture.completedFuture(
                        userCtrl == null
                                ? error("User controller is not configured")
                                : userCtrl.register(payload));
                case EventType.UPDATE_PROFILE -> CompletableFuture.completedFuture(
                        userCtrl == null
                                ? error("User controller is not configured")
                                : userCtrl.updateProfile(payload));
                case EventType.CHANGE_PASSWORD -> CompletableFuture.completedFuture(
                        userCtrl == null
                                ? error("User controller is not configured")
                                : userCtrl.changePassword(payload));
                case EventType.LOGOUT -> CompletableFuture.completedFuture(
                        userCtrl == null
                                ? error("User controller is not configured")
                                : userCtrl.logout(payload));
                case EventType.DEPOSIT -> userCtrl == null
                        ? CompletableFuture.completedFuture(
                                error("User controller is not configured"))
                        : userCtrl.deposit(payload);
                case EventType.WITHDRAW -> userCtrl == null
                        ? CompletableFuture.completedFuture(
                                error("User controller is not configured"))
                        : userCtrl.withdraw(payload);
                case EventType.GET_WALLET_TRANSACTIONS -> userCtrl == null
                        ? CompletableFuture.completedFuture(
                                error("User controller is not configured"))
                        : userCtrl.getWalletTransactions(sessionUserId);
                case EventType.PLACE_BID -> bidCtrl == null
                        ? CompletableFuture.completedFuture(
                                error("Bid controller is not configured"))
                        : bidCtrl.placeBid(payload);
                case EventType.CREATE_AUCTION -> CompletableFuture.completedFuture(
                        auctionCtrl == null
                                ? error("Auction controller is not configured")
                                : auctionCtrl.createAuction(payload));
                case EventType.GET_AUCTION_DETAILS -> CompletableFuture.completedFuture(
                        auctionCtrl == null
                                ? error("Auction controller is not configured")
                                : auctionCtrl.getAuctionDetails(payload));
                case EventType.GET_AUCTIONS_BY_SELLER -> CompletableFuture.completedFuture(
                        auctionCtrl == null
                                ? error("Auction controller is not configured")
                                : auctionCtrl.getAuctionsBySellerId(payload));
                case EventType.GET_BIDS_BY_AUCTION_ID -> CompletableFuture.completedFuture(
                        bidCtrl == null
                                ? error("Bid controller is not configured")
                                : bidCtrl.getBidsByAuctionId(payload));
                case EventType.GET_BIDS_BY_BIDDER_ID -> CompletableFuture.completedFuture(
                        bidCtrl == null
                                ? error("Bid controller is not configured")
                                : bidCtrl.getBidsByBidderId(payload));
                case EventType.GET_PUBLIC_AUCTIONS -> CompletableFuture.completedFuture(
                        auctionCtrl == null
                                ? error("Auction controller is not configured")
                                : auctionCtrl.getPublicAuctions(payload));
                case EventType.GET_FEATURED_AUCTIONS -> CompletableFuture.completedFuture(
                        auctionCtrl == null
                                ? error("Auction controller is not configured")
                                : auctionCtrl.getFeaturedAuctions(payload));
                case EventType.PROMOTE_AUCTION -> CompletableFuture.completedFuture(
                        auctionCtrl == null
                                ? error("Auction controller is not configured")
                                : auctionCtrl.promoteAuction(payload));
                case EventType.GET_UPLOAD_SIGNATURE -> CompletableFuture.completedFuture(
                        itemCtrl == null
                                ? error("Item controller is not configured")
                                : itemCtrl.getUploadSignature(payload));
                case EventType.GET_ALL_AUCTIONS_ADMIN -> {
                    // Verify Admin privileges on the Server side before dispatching
                    if (!isAdminSession(sessionUserId)) {
                        yield CompletableFuture.completedFuture(
                                error("Forbidden: Admin access required."));
                    }
                    yield CompletableFuture.completedFuture(
                            auctionCtrl == null
                                    ? error("Auction controller is not configured")
                                    : auctionCtrl.getAllAuctionsForAdmin(payload));
                }
                case EventType.GET_ALL_USERS_ADMIN -> {
                    if (!isAdminSession(sessionUserId)) {
                        yield CompletableFuture.completedFuture(
                                error("Forbidden: Admin access required."));
                    }
                    yield CompletableFuture.completedFuture(
                            userCtrl == null
                                    ? error("User controller is not configured")
                                    : userCtrl.getAllUsersForAdmin(payload));
                }
                case EventType.TOGGLE_USER_STATUS_ADMIN -> {
                    if (!isAdminSession(sessionUserId)) {
                        yield CompletableFuture.completedFuture(
                                error("Forbidden: Admin access required."));
                    }
                    yield CompletableFuture.completedFuture(
                            userCtrl == null
                                    ? error("User controller is not configured")
                                    : userCtrl.toggleUserStatus(payload));
                }
                default -> CompletableFuture.completedFuture(
                        error("Endpoint not implemented: " + type.name()));
            };
        } catch (Exception ex) {
            System.err.println("Unhandled exception during dispatch: " + ex.getMessage());
            ex.printStackTrace();
            responseFuture = CompletableFuture.completedFuture(ApiResponse.error("Internal Server Error"));
        }

        // Enrich response with type and correlationId
        return responseFuture.thenApply(raw -> enrichResponse(raw, type, correlationId));
    }

    /**
     * Shorthand so callers inside this class can write {@code error("...")} instead
     * of {@code
     * ApiResponse.error("...")}.
     */
    private String error(String message) {
        return ApiResponse.error(message);
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
            case LOGIN,
                    REGISTER,
                    GET_AUCTION_DETAILS,
                    GET_BIDS_BY_AUCTION_ID,
                    GET_PUBLIC_AUCTIONS,
                    GET_FEATURED_AUCTIONS ->
                true;
            default -> false;
        };
    }

    /**
     * Check if the connected user is an Admin by querying the database. Clients
     * cannot forge this
     * because the Server verifies it using the userId from the session.
     */
    private boolean isAdminSession(Integer sessionUserId) {
        if (sessionUserId == null || userDao == null) {
            return false;
        }
        try {
            com.auction.core.users.User user = userDao.findById(sessionUserId);
            return user != null && user.getRole() == com.auction.core.users.User.Role.ADMIN;
        } catch (Exception e) {
            return false;
        }
    }

    private void overridePayloadIdentity(
            EventType type, Map<String, Object> payload, Integer sessionUserId) {
        switch (type) {
            case PLACE_BID, GET_BIDS_BY_BIDDER_ID -> payload.put("bidderId", sessionUserId);
            case UPDATE_PROFILE,
                    CHANGE_PASSWORD,
                    LOGOUT,
                    DEPOSIT,
                    WITHDRAW,
                    GET_WALLET_TRANSACTIONS ->
                payload.put("userId", sessionUserId);
            case CREATE_AUCTION -> payload.put("sellerId", sessionUserId);
            case GET_AUCTIONS_BY_SELLER -> {
                if (!payload.containsKey("sellerId")) {
                    payload.put("sellerId", sessionUserId);
                }
            }
            case PROMOTE_AUCTION -> payload.put(
                    "sellerId", sessionUserId); // server đè lên, bảo mật
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
