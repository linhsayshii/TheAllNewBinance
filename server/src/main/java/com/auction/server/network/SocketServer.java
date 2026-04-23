package com.auction.server.network;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.auction.core.protocol.EventType;
import com.auction.core.utils.JsonMapper;
import com.auction.server.controller.RequestDispatcher;

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
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Connection closed: " + conn.getRemoteSocketAddress() + " with code " + code);
        userSessions.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Integer userId = userSessions.get(conn);
        String response = dispatcher.dispatch(userId, message);
        conn.send(response);

        // Tự động map/unmap Session Socket dựa trên kết quả LOGIN/REGISTER/LOGOUT
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
                        System.out.println("Authenticated connection: " + conn.getRemoteSocketAddress() + " -> UserId: " + id);
                    }
                }
            } else if (userId != null && type == EventType.LOGOUT) {
                Map<?, ?> respNode = JsonMapper.fromJson(response, Map.class);
                if (Boolean.TRUE.equals(respNode.get("success"))) {
                    userSessions.remove(conn);
                    System.out.println("User logged out on connection: " + conn.getRemoteSocketAddress());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to intercept auth/session packet: " + e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Error on connection " + (conn != null ? conn.getRemoteSocketAddress() : "null") + ": " + ex.getMessage());
        if (conn != null) {
            userSessions.remove(conn);
        }
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket Server started successfully on port: " + getPort());
    }
}
