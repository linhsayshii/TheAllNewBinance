package com.auction.client.service;

import com.auction.client.config.AppConfig;
import com.auction.client.mock.MockAuctionClient;
import com.auction.client.mock.MockDataProvider;
import com.auction.client.network.AuctionClient;
import com.auction.core.protocol.EventType;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class NetworkService {

    private static NetworkService instance;
    private final AuctionClient client;

    /** Shared MockDataProvider so ClientApp can reuse it for auto-login. */
    private MockDataProvider mockDataProvider;

    private NetworkService(String serverUri) {
        try {
            if (AppConfig.isMockMode()) {
                mockDataProvider = new MockDataProvider();
                client = new MockAuctionClient(mockDataProvider);
                System.out.println(
                        "[MockMode] NetworkService using MockAuctionClient — no server connection");
            } else {
                client = new AuctionClient(new URI(serverUri));
                client.connect();
                // Bắt đầu đồng bộ giờ chuẩn UTC+7 ngay sau khi kết nối server
                TimeSyncService.syncTime();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize NetworkService", e);
        }
    }

    public static synchronized void init(String serverUri) {
        if (instance == null) {
            instance = new NetworkService(serverUri);
        }
    }

    public static NetworkService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("NetworkService not initialized. Call init() first.");
        }
        return instance;
    }

    /**
     * Resets the singleton for test isolation. Only for use in unit/integration tests.
     * Must be called with app.mockMode=true set before calling init() again.
     */
    public static void resetForTest() {
        instance = null;
    }

    public AuctionClient getClient() {
        return client;
    }

    public String sendRequest(EventType type, Object payload) {
        return client.sendRequest(type, payload);
    }

    public void addCorrelationHandler(
            String correlationId, java.util.function.Consumer<String> handler) {
        client.addCorrelationHandler(correlationId, handler);
    }

    public void removeCorrelationHandler(String correlationId) {
        client.removeCorrelationHandler(correlationId);
    }

    /**
     * Sends a request and returns a self-cleaning {@link CompletableFuture}. The correlation
     * handler is automatically removed once the future completes, guaranteeing zero memory leaks
     * even when the server is slow or unreachable.
     */
    public CompletableFuture<String> sendRequestAsync(EventType type, Object payload) {
        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            String correlationId = sendRequest(type, payload);
            addCorrelationHandler(correlationId, future::complete);
            future.whenComplete((res, ex) -> removeCorrelationHandler(correlationId));
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * Returns the MockDataProvider if running in mock mode, otherwise null. Used by ClientApp for
     * auto-login of the default mock user.
     */
    public MockDataProvider getMockDataProvider() {
        return mockDataProvider;
    }

    /** Gracefully close socket and stop reconnect executor on app shutdown. */
    public void shutdown() {
        try {
            client.closeBlocking();
        } catch (Exception e) {
            System.err.println("Error shutting down NetworkService: " + e.getMessage());
        } finally {
            client.stopReconnectExecutor();
        }
    }
}
