package com.auction.client.network;

import com.auction.core.protocol.EventType;
import com.auction.core.utils.JsonMapper;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class AuctionClient extends WebSocketClient {

    // Map: eventType -> (handlerId -> Handler)
    protected final Map<EventType, Map<String, Consumer<String>>> typedHandlers =
            new ConcurrentHashMap<>();

    // Map: correlationId -> One-time Handler
    protected final Map<String, Consumer<String>> correlationHandlers = new ConcurrentHashMap<>();

    private final ScheduledExecutorService reconnectExecutor =
            Executors.newSingleThreadScheduledExecutor();
    private boolean isReconnecting = false;

    public AuctionClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Connected to Auction Server: " + getURI());
        isReconnecting = false;
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Received message: " + message);
        try {
            Map<?, ?> node = JsonMapper.fromJson(message, Map.class);

            // 1. Handle by Correlation ID (One-time callback)
            Object corrIdObj = node.get("correlationId");
            if (corrIdObj != null) {
                String correlationId = String.valueOf(corrIdObj);
                Consumer<String> handler = correlationHandlers.remove(correlationId);
                if (handler != null) {
                    handler.accept(message);
                }
            }

            // 2. Handle by Event Type (Persistent subscribers)
            EventType type = EventType.fromWireValue(node.get("type"));
            if (type != null && typedHandlers.containsKey(type)) {
                Map<String, Consumer<String>> handlers = typedHandlers.get(type);
                if (handlers != null) {
                    for (Consumer<String> handler : handlers.values()) {
                        handler.accept(message);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing message: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Connection closed: " + reason);
        triggerReconnect();
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("WebSocket Error: " + ex.getMessage());
    }

    public void triggerReconnect() {
        if (!isReconnecting && !isOpen()) {
            isReconnecting = true;
            reconnectExecutor.schedule(
                    () -> {
                        System.out.println("Attempting to reconnect...");
                        try {
                            if (reconnectBlocking()) {
                                System.out.println("Reconnected successfully.");
                            } else {
                                isReconnecting = false;
                                triggerReconnect();
                            }
                        } catch (Exception e) {
                            System.err.println("Reconnect attempt failed.");
                            isReconnecting = false;
                            triggerReconnect();
                        }
                    },
                    5,
                    TimeUnit.SECONDS);
        }
    }

    /**
     * Gửi một request JSON tới server và tự động gán correlationId
     *
     * @return correlationId được sinh ra
     */
    public String sendRequest(EventType type, Object payload) {
        String correlationId = java.util.UUID.randomUUID().toString();
        sendRequest(type, correlationId, payload);
        return correlationId;
    }

    /** Gửi request với correlationId cụ thể */
    public void sendRequest(EventType type, String correlationId, Object payload) {
        if (!isOpen()) {
            System.err.println("Cannot send request: Socket is not open");
            return;
        }
        Map<String, Object> request =
                Map.of(
                        "type", type.wireValue(),
                        "correlationId", correlationId,
                        "payload", payload);
        String json = JsonMapper.toJson(request);
        send(json);
    }

    /** Đăng ký một callback duy nhất cho một correlationId cụ thể */
    public void addCorrelationHandler(String correlationId, Consumer<String> handler) {
        correlationHandlers.put(correlationId, handler);
    }

    /** Hủy đăng ký callback của một correlationId cụ thể */
    public void removeCorrelationHandler(String correlationId) {
        correlationHandlers.remove(correlationId);
    }

    /**
     * @param eventType the type of event to listen to (e.g., EventType.GET_BIDS_BY_AUCTION_ID)
     * @param handlerId a unique id for this handler (e.g. Component Name)
     * @param handler the callback
     */
    public void addResponseHandler(
            EventType eventType, String handlerId, Consumer<String> handler) {
        typedHandlers
                .computeIfAbsent(eventType, k -> new ConcurrentHashMap<>())
                .put(handlerId, handler);
    }

    public void removeResponseHandler(EventType eventType, String handlerId) {
        Map<String, Consumer<String>> handlers = typedHandlers.get(eventType);
        if (handlers != null) {
            handlers.remove(handlerId);
            if (handlers.isEmpty()) {
                typedHandlers.remove(eventType);
            }
        }
    }

    /** Stop the reconnect scheduler when the app is shutting down. */
    public void stopReconnectExecutor() {
        reconnectExecutor.shutdownNow();
    }
}
