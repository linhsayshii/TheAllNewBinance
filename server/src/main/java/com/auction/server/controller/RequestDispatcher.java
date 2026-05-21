package com.auction.server.controller;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.auction.core.protocol.EventType;
import com.auction.core.utils.JsonMapper;
import com.auction.server.dao.impl.IUserDao;

public class RequestDispatcher {
    private final UserController userCtrl;
    private final AuctionController auctionCtrl;
    private final BidController bidCtrl;
    private final ItemController itemCtrl;
    private final IUserDao userDao; // dùng để xác minh quyền Admin phía Server

    public RequestDispatcher(UserController userCtrl, AuctionController auctionCtrl, BidController bidCtrl, ItemController itemCtrl, IUserDao userDao) {
        this.userCtrl = userCtrl;
        this.auctionCtrl = auctionCtrl;
        this.bidCtrl = bidCtrl;
        this.itemCtrl = itemCtrl;
        this.userDao = userDao;
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

        String responseRaw;
        try {
            responseRaw = switch (type) {
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
                case EventType.LOGOUT -> userCtrl == null
                        ? error("User controller is not configured")
                        : userCtrl.logout(payload);
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
                case EventType.GET_FEATURED_AUCTIONS -> auctionCtrl == null
                        ? error("Auction controller is not configured")
                        : auctionCtrl.getFeaturedAuctions(payload);
                case EventType.PROMOTE_AUCTION -> auctionCtrl == null
                        ? error("Auction controller is not configured")
                        : auctionCtrl.promoteAuction(payload);
                case EventType.GET_UPLOAD_SIGNATURE -> itemCtrl == null
                        ? error("Item controller is not configured")
                        : itemCtrl.getUploadSignature(payload);
                case EventType.GET_ALL_AUCTIONS_ADMIN -> {
                    // Kiểm tra quyền Admin phía Server trước khi xử lý
                    if (!isAdminSession(sessionUserId)) {
                        yield error("Forbidden: Admin access required.");
                    }
                    yield auctionCtrl == null
                            ? error("Auction controller is not configured")
                            : auctionCtrl.getAllAuctionsForAdmin(payload);
                }
                default -> error("Endpoint not implemented: " + type.name());
            };
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
            case LOGIN, REGISTER, GET_AUCTION_DETAILS, GET_BIDS_BY_AUCTION_ID,
                 GET_PUBLIC_AUCTIONS, GET_FEATURED_AUCTIONS -> true;
            default -> false;
        };
    }

    /**
     * Kiểm tra user đang kết nối có phải Admin không bằng cách query DB thực tế.
     * Client không thể giả mạo vì Server tự kiểm tra với userId từ Session.
     */
    private boolean isAdminSession(Integer sessionUserId) {
        if (sessionUserId == null || userDao == null) return false;
        try {
            com.auction.core.users.User user = userDao.findById(sessionUserId);
            return user != null && user.getRole() == com.auction.core.users.User.Role.ADMIN;
        } catch (Exception e) {
            return false;
        }
    }

    private void overridePayloadIdentity(EventType type, Map<String, Object> payload, Integer sessionUserId) {
        switch (type) {
            case PLACE_BID, GET_BIDS_BY_BIDDER_ID -> payload.put("bidderId", sessionUserId);
            case UPDATE_PROFILE, CHANGE_PASSWORD, LOGOUT -> payload.put("userId", sessionUserId);
            case CREATE_AUCTION, GET_AUCTIONS_BY_SELLER -> payload.put("sellerId", sessionUserId);
            case PROMOTE_AUCTION -> payload.put("sellerId", sessionUserId); // server đè lên, bảo mật
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
