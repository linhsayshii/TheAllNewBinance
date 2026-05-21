package com.auction.client.mock;

import com.auction.client.network.AuctionClient;
import com.auction.core.auction.Bid;
import com.auction.core.protocol.EventType;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javafx.application.Platform;

/**
 * A fake {@link AuctionClient} that never connects to a real WebSocket server.
 *
 * <p>When {@link #sendRequest} is called, it delegates to {@link MockDataProvider} to obtain a
 * pre-built JSON response, then fires the appropriate correlation and typed handlers after a short
 * simulated network delay (50–200 ms).
 *
 * <p>All existing handler registration methods ({@code addCorrelationHandler}, {@code
 * addResponseHandler}, etc.) are inherited unchanged from {@link AuctionClient} — only the
 * sending/connecting side is overridden.
 */
public class MockAuctionClient extends AuctionClient {

    /** Simulated min network latency in milliseconds. */
    private static final long DELAY_MIN_MS = 50;

    /** Simulated max network latency in milliseconds. */
    private static final long DELAY_MAX_MS = 200;

    private final MockDataProvider dataProvider;
    private final ScheduledExecutorService scheduler;

    /** Dummy URI — passed to super() but never actually connected to. */
    private static final URI DUMMY_URI = URI.create("ws://localhost:0");

    public MockAuctionClient(MockDataProvider dataProvider) {
        super(DUMMY_URI);
        this.dataProvider = dataProvider;
        this.scheduler =
                Executors.newSingleThreadScheduledExecutor(
                        r -> {
                            Thread t = new Thread(r, "mock-network-thread");
                            t.setDaemon(true);
                            return t;
                        });
        startSimulatedBidders();
    }

    // ------------------------------------------------------------------ //
    //  Connection overrides — NO-OP                                        //
    // ------------------------------------------------------------------ //

    @Override
    public void connect() {
        // Intentionally does nothing — no real WebSocket connection is made.
        System.out.println("[MockMode] MockAuctionClient active — no server connection");
    }

    @Override
    public boolean connectBlocking() {
        return true;
    }

    @Override
    public boolean reconnectBlocking() {
        return true;
    }

    @Override
    public boolean isOpen() {
        // Always report "open" so controllers don't block waiting for socket
        return true;
    }

    @Override
    public void closeBlocking() {
        scheduler.shutdownNow();
    }

    @Override
    public void stopReconnectExecutor() {
        scheduler.shutdownNow();
    }

    // ------------------------------------------------------------------ //
    //  Request interception                                                //
    // ------------------------------------------------------------------ //

    /**
     * Overrides WebSocket send — builds a mock response and fires it asynchronously after a
     * simulated delay.
     */
    @Override
    public String sendRequest(EventType type, Object payload) {
        String correlationId = UUID.randomUUID().toString();
        sendRequest(type, correlationId, payload);
        return correlationId;
    }

    @Override
    public void sendRequest(EventType type, String correlationId, Object payload) {
        long delayMs = ThreadLocalRandom.current().nextLong(DELAY_MIN_MS, DELAY_MAX_MS);

        scheduler.schedule(
                () -> {
                    String response = dataProvider.buildResponse(type, payload);
                    System.out.println("[MockMode] " + type + " → " + truncate(response, 120));

                    // 1. Fire one-time correlation handler (most requests use this path)
                    Consumer<String> corrHandler = correlationHandlers.remove(correlationId);
                    if (corrHandler != null) {
                        corrHandler.accept(response);
                    }

                    // 2. Fire persistent typed handlers (e.g. AuctionPage listens for
                    //    GET_BIDS_BY_AUCTION_ID and PLACE_BID via addResponseHandler)
                    Map<String, Consumer<String>> handlers = typedHandlers.get(type);
                    if (handlers != null) {
                        for (Consumer<String> h : handlers.values()) {
                            h.accept(response);
                        }
                    }
                },
                delayMs,
                TimeUnit.MILLISECONDS);
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    private static String truncate(String s, int max) {
        if (s == null) return "(null)";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private void startSimulatedBidders() {
        // Cứ mỗi 10 giây sẽ kiểm tra và có 60% cơ hội sinh ra một lượt đặt giá tự động từ người
        // dùng khác
        scheduler.scheduleAtFixedRate(
                () -> {
                    try {
                        if (ThreadLocalRandom.current().nextDouble() > 0.6) {
                            return;
                        }

                        Bid simulatedBid = dataProvider.generateSimulatedBidFromOtherUser();
                        if (simulatedBid != null) {
                            // Tạo phản hồi JSON giống hệt server gửi về khi có ai đó PLACE_BID
                            Map<String, Object> response = new java.util.LinkedHashMap<>();
                            response.put("success", true);
                            response.put("message", "Simulated other user bid");
                            response.put("data", simulatedBid);
                            String responseJson =
                                    com.auction.core.utils.JsonMapper.toJson(response);

                            // Phát sự kiện tới tất cả các handler đang lắng nghe PLACE_BID ở Client
                            Map<String, Consumer<String>> handlers =
                                    typedHandlers.get(EventType.PLACE_BID);
                            if (handlers != null) {
                                for (Consumer<String> h : handlers.values()) {
                                    Platform.runLater(() -> h.accept(responseJson));
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println(
                                "[MockMode] Error in simulated bidder thread: " + e.getMessage());
                    }
                },
                8,
                10,
                TimeUnit.SECONDS);
    }
}
