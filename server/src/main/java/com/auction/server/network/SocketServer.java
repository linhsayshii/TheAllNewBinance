package com.auction.server.network;

import com.auction.core.protocol.EventType;
import com.auction.core.utils.JsonMapper;
import com.auction.server.controller.RequestDispatcher;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class SocketServer extends WebSocketServer {

    private final RequestDispatcher dispatcher;
    private final Map<WebSocket, Integer> userSessions = new ConcurrentHashMap<>();

    public SocketServer(int port, RequestDispatcher dispatcher) {
        super(new InetSocketAddress(port));
        this.dispatcher = dispatcher;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection: " + conn.getRemoteSocketAddress());
        BroadcastBroker.getInstance().addConnection(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println(
                "Connection closed: " + conn.getRemoteSocketAddress() + " with code " + code);
        userSessions.remove(conn);
        BroadcastBroker.getInstance().removeConnection(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Đánh chặn SUBSCRIBE/UNSUBSCRIBE trực tiếp tại tầng socket, không qua Dispatcher
        if (interceptRoomSubscription(conn, message)) {
            return;
        }

        Integer userId = userSessions.get(conn);

        // dispatch() trả về CompletableFuture — full async
        dispatcher
                .dispatch(userId, message)
                .thenAccept(
                        response -> {
                            if (conn.isOpen()) {
                                conn.send(response);
                            }

                            // Tự động map/unmap Session Socket dựa trên kết quả
                            // LOGIN/REGISTER/LOGOUT
                            interceptAuthSession(conn, userId, message, response);

                            // Phát sóng kết quả đặt giá thành công tới tất cả client trong phòng
                            interceptBidBroadcast(conn, message, response);
                            interceptPromotionBroadcast(message, response);
                        })
                .exceptionally(
                        ex -> {
                            System.err.println("Async dispatch error: " + ex.getMessage());
                            if (conn.isOpen()) {
                                conn.send(
                                        "{\"success\":false,\"message\":\"Internal server"
                                                + " error\"}");
                            }
                            return null;
                        });
    }

    /**
     * Đánh chặn yêu cầu đăng ký/hủy đăng ký phòng đấu giá. Trả về {@code true} nếu gói tin đã được
     * xử lý và không cần chuyển tiếp sang Dispatcher.
     */
    private boolean interceptRoomSubscription(WebSocket conn, String message) {
        try {
            Map<?, ?> node = JsonMapper.fromJson(message, Map.class);
            EventType type = EventType.fromWireValue(node.get("type"));

            if (type == EventType.SUBSCRIBE_AUCTION) {
                Object payload = node.get("payload");
                if (payload instanceof Map<?, ?> payloadMap) {
                    Object auctionIdObj = payloadMap.get("auctionId");
                    if (auctionIdObj instanceof Number) {
                        BroadcastBroker.getInstance()
                                .register(((Number) auctionIdObj).intValue(), conn);
                    }
                }
                return true;
            }

            if (type == EventType.UNSUBSCRIBE_AUCTION) {
                BroadcastBroker.getInstance().unregister(conn);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Failed to intercept room subscription packet: " + e.getMessage());
        }
        return false;
    }

    /**
     * Sau khi đặt giá thành công, phát sóng kết quả tới tất cả các client đang xem cùng phiên đấu
     * giá đó (trừ người gửi đã nhận Unicast response kèm correlationId).
     */
    private void interceptBidBroadcast(WebSocket conn, String request, String response) {
        try {
            Map<?, ?> reqNode = JsonMapper.fromJson(request, Map.class);
            EventType type = EventType.fromWireValue(reqNode.get("type"));
            if (type != EventType.PLACE_BID) {
                return;
            }

            Map<?, ?> respNode = JsonMapper.fromJson(response, Map.class);
            if (!Boolean.TRUE.equals(respNode.get("success"))) {
                return;
            }

            Object data = respNode.get("data");
            if (data == null) {
                return;
            }

            Object payloadObj = reqNode.get("payload");
            if (!(payloadObj instanceof Map<?, ?> payloadMap)) {
                return;
            }
            Object auctionIdObj = payloadMap.get("auctionId");
            if (!(auctionIdObj instanceof Number)) {
                return;
            }
            int auctionId = ((Number) auctionIdObj).intValue();

            BroadcastBroker.getInstance()
                    .broadcastToRoom(auctionId, EventType.PLACE_BID, data, conn);
        } catch (Exception e) {
            System.err.println("Failed to intercept bid broadcast: " + e.getMessage());
        }
    }

    /** Intercept auth-related responses to manage session mapping. */
    private void interceptAuthSession(
            WebSocket conn, Integer userId, String message, String response) {
        try {
            Map<?, ?> reqNode = JsonMapper.fromJson(message, Map.class);
            EventType type = EventType.fromWireValue(reqNode.get("type"));

            if (userId == null && (type == EventType.LOGIN || type == EventType.REGISTER)) {
                Map<?, ?> respNode = JsonMapper.fromJson(response, Map.class);
                if (Boolean.TRUE.equals(respNode.get("success"))) {
                    Map<?, ?> data = (Map<?, ?>) respNode.get("data");
                    if (data != null && data.containsKey("id")) {
                        Number id = (Number) data.get("id");
                        userSessions.put(conn, id.intValue());
                        BroadcastBroker.getInstance().registerUser(id.intValue(), conn);
                        System.out.println(
                                "Authenticated connection: "
                                        + conn.getRemoteSocketAddress()
                                        + " -> UserId: "
                                        + id);
                    }
                }
            } else if (userId != null && type == EventType.LOGOUT) {
                Map<?, ?> respNode = JsonMapper.fromJson(response, Map.class);
                if (Boolean.TRUE.equals(respNode.get("success"))) {
                    userSessions.remove(conn);
                    BroadcastBroker.getInstance().unregisterUser(userId, conn);
                    System.out.println(
                            "User logged out on connection: " + conn.getRemoteSocketAddress());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to intercept auth/session packet: " + e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println(
                "Error on connection "
                        + (conn != null ? conn.getRemoteSocketAddress() : "null")
                        + ": "
                        + ex.getMessage());
        if (conn != null) {
            userSessions.remove(conn);
            BroadcastBroker.getInstance().removeConnection(conn);
        }
    }

    private void interceptPromotionBroadcast(String request, String response) {
        try {
            Map<?, ?> reqNode = JsonMapper.fromJson(request, Map.class);
            EventType type = EventType.fromWireValue(reqNode.get("type"));
            if (type != EventType.PROMOTE_AUCTION) {
                return;
            }

            Map<?, ?> respNode = JsonMapper.fromJson(response, Map.class);
            if (Boolean.TRUE.equals(respNode.get("success"))) {
                BroadcastBroker.getInstance()
                        .broadcastToRoom(
                                0, EventType.PROMOTE_AUCTION, Map.of("success", true), null);
            }
        } catch (Exception e) {
            System.err.println("Failed to intercept promotion broadcast: " + e.getMessage());
        }
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket Server started successfully on port: " + getPort());
    }
}
