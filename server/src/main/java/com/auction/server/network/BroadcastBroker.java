package com.auction.server.network;

import com.auction.core.protocol.EventType;
import com.auction.core.utils.JsonMapper;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.java_websocket.WebSocket;

/**
 * Manages scoped pub/sub rooms for realtime auction broadcast.
 *
 * <p>Each auction session gets its own room (keyed by auctionId). Connections register when a
 * client opens an auction detail page and unregister when they leave or disconnect. The Singleton
 * instance is thread-safe via ConcurrentHashMap and CopyOnWriteArraySet.
 */
public final class BroadcastBroker {

    private static final BroadcastBroker INSTANCE = new BroadcastBroker();

    /** Global set of all connected clients */
    private final Set<WebSocket> activeConnections =
            new java.util.concurrent.CopyOnWriteArraySet<>();

    /** Maps userId -> set of active WebSocket connections */
    private final Map<Integer, Set<WebSocket>> userConnections = new ConcurrentHashMap<>();

    /** auctionId → set of active WebSocket connections watching that auction. */
    private final Map<Integer, Set<WebSocket>> rooms = new ConcurrentHashMap<>();

    /** Reverse-index: connection → auctionId, used for O(1) cleanup on disconnect. */
    private final Map<WebSocket, Integer> userLocations = new ConcurrentHashMap<>();

    private BroadcastBroker() {}

    public static BroadcastBroker getInstance() {
        return INSTANCE;
    }

    public void addConnection(WebSocket conn) {
        activeConnections.add(conn);
    }

    public void removeConnection(WebSocket conn) {
        activeConnections.remove(conn);
        unregister(conn);
        userConnections.values().forEach(set -> set.remove(conn));
    }

    public void registerUser(int userId, WebSocket conn) {
        userConnections
                .computeIfAbsent(userId, k -> new java.util.concurrent.CopyOnWriteArraySet<>())
                .add(conn);
        System.out.println("[BroadcastBroker] Registered user ID: " + userId);
    }

    public void unregisterUser(int userId, WebSocket conn) {
        Set<WebSocket> conns = userConnections.get(userId);
        if (conns != null) {
            conns.remove(conn);
            if (conns.isEmpty()) {
                userConnections.remove(userId);
            }
        }
        System.out.println("[BroadcastBroker] Unregistered user ID: " + userId);
    }

    public void sendToUser(int userId, EventType type, Object payload) {
        Set<WebSocket> conns = userConnections.get(userId);
        if (conns == null || conns.isEmpty()) {
            return;
        }
        Map<String, Object> message =
                Map.of("type", type.wireValue(), "success", true, "data", payload);
        String json = JsonMapper.toJson(message);
        for (WebSocket conn : conns) {
            if (conn.isOpen()) {
                conn.send(json);
            }
        }
    }

    /**
     * Gửi sự kiện FORCE_LOGOUT_ADMIN tới tất cả các kết nối WebSocket của người dùng bị
     * khóa, sau đó đóng kết nối với mã 1008 (Policy Violation). Sử dụng shallow copy
     * để tránh ConcurrentModificationException khi onClose() xóa phần tử khỏi Set.
     */
    public void forceLogoutAndDisconnectUser(int userId) {
        final Set<WebSocket> conns = userConnections.get(userId);
        if (conns == null || conns.isEmpty()) {
            return;
        }
        final Map<String, Object> message =
                Map.of(
                        "type", EventType.FORCE_LOGOUT_ADMIN.wireValue(),
                        "success", true,
                        "data", Map.of("reason", "banned"));
        final String json = JsonMapper.toJson(message);
        final java.util.List<WebSocket> shallowCopy = new java.util.ArrayList<>(conns);
        for (final WebSocket conn : shallowCopy) {
            if (conn.isOpen()) {
                try {
                    conn.send(json);
                    conn.close(1008, "Banned by Admin");
                } catch (Exception e) {
                    System.err.println(
                            "[BroadcastBroker] Lỗi khi ngắt kết nối user "
                                    + userId
                                    + ": "
                                    + e.getMessage());
                }
            }
        }
    }

    /**
     * Registers a WebSocket connection into an auction room. If the connection was previously
     * watching a different room, it is automatically evicted from the old room first.
     *
     * @param auctionId target auction room
     * @param conn WebSocket connection to register
     */
    public void register(int auctionId, WebSocket conn) {
        unregister(conn);
        rooms.computeIfAbsent(auctionId, k -> new CopyOnWriteArraySet<>()).add(conn);
        userLocations.put(conn, auctionId);
        System.out.println("[BroadcastBroker] Registered conn to room: " + auctionId);
    }

    /**
     * Removes a WebSocket connection from whichever room it currently belongs to. Safe to call even
     * if the connection is not in any room. Called both on explicit UNSUBSCRIBE and on global
     * onClose/onError to prevent memory leaks from dead connections.
     *
     * @param conn WebSocket connection to remove
     */
    public void unregister(WebSocket conn) {
        Integer activeRoom = userLocations.remove(conn);
        if (activeRoom != null) {
            Set<WebSocket> room = rooms.get(activeRoom);
            if (room != null) {
                room.remove(conn);
                if (room.isEmpty()) {
                    rooms.remove(activeRoom);
                }
            }
            System.out.println("[BroadcastBroker] Unregistered conn from room: " + activeRoom);
        }
    }

    /**
     * Broadcasts a rich payload to all open connections in the target auction room, excluding the
     * original sender (which already received a direct Unicast response with correlationId).
     *
     * <p>The broadcast message intentionally omits {@code correlationId} so that receiving clients
     * can distinguish it from their own unicast response and avoid clearing the bid input field.
     *
     * @param auctionId target room
     * @param type event type label embedded in the push packet
     * @param payload the data object to broadcast (will be serialised to JSON)
     * @param excludeConn the sender connection to skip
     */
    public void broadcastToRoom(
            int auctionId, EventType type, Object payload, WebSocket excludeConn) {
        Set<WebSocket> room = rooms.get(auctionId);
        if (room == null || room.isEmpty()) {
            return;
        }

        Map<String, Object> message =
                Map.of("type", type.wireValue(), "success", true, "data", payload);
        String json = JsonMapper.toJson(message);

        for (WebSocket conn : room) {
            if (conn != excludeConn && conn.isOpen()) {
                conn.send(json);
            }
        }
    }

    /**
     * Broadcasts a message to every open connection across all rooms.
     *
     * @param type event type label embedded in the push packet
     * @param payload the data object to broadcast
     */
    public void broadcastToAll(EventType type, Object payload) {
        Map<String, Object> message =
                Map.of("type", type.wireValue(), "success", true, "data", payload);
        String json = JsonMapper.toJson(message);

        for (WebSocket conn : activeConnections) {
            if (conn.isOpen()) {
                conn.send(json);
            }
        }
    }
}
