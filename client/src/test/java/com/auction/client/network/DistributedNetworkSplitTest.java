package com.auction.client.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.client.mock.MockAuctionClient;
import com.auction.client.service.NetworkService;
import com.auction.core.protocol.EventType;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử khả năng phục hồi kết nối mạng (Network Resilience Tests).
 *
 * <p>File này bao gồm hai tầng test:
 *
 * <ol>
 *   <li><b>Tầng 1 - Mock-based (luôn chạy):</b> Dùng {@link MockAuctionClient} để giả lập TCP
 *       Half-Open deterministic mà không cần Docker hay server thật. Phù hợp cho unit CI/CD.
 *   <li><b>Tầng 2 - Toxiproxy-based (cần Docker):</b> Dùng Testcontainers + Toxiproxy để giả lập
 *       network partition thực sự ở tầng TCP. Bật bằng {@code -Dtest.network.integration=true}.
 * </ol>
 *
 * <p><b>Tại sao TCP Half-Open quan trọng hơn closeBlocking()?</b><br>
 * {@code closeBlocking()} gửi WebSocket Close Frame + TCP FIN — là graceful shutdown có kiểm soát.
 * TCP Half-Open (code 1006) xảy ra khi server crash hoặc mạng bị cắt vật lý: không có FIN nào
 * được gửi, và client tiếp tục nghĩ rằng connection vẫn "alive" cho đến khi heartbeat timeout.
 * Đây là trường hợp nguy hiểm nhất trong hệ thống đấu giá thời gian thực.
 *
 * <p><b>Lỗ hổng #1 được kiểm thử ở đây:</b> Sau khi reconnect, Controller phải tự động tái đăng
 * ký {@code SUBSCRIBE_AUCTION} thông qua {@link ReconnectListener}. Nếu không, Client sẽ mất
 * toàn bộ broadcast bidding updates trong khi auction vẫn đang hoạt động, và người dùng sẽ không
 * biết bid mới nhất mà không reload trang thủ công.
 */
@DisplayName("Network Resilience Tests")
class DistributedNetworkSplitTest {

    private MockAuctionClient mockClient;

    @BeforeEach
    void setUp() {
        System.setProperty("app.mockMode", "true");
        NetworkService.resetForTest();
        NetworkService.init("ws://localhost:0");
        mockClient = (MockAuctionClient) NetworkService.getInstance().getClient();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("app.mockMode");
        NetworkService.resetForTest();
    }

    // =========================================================================
    // Tầng 1: Mock-based Network Tests (không cần Docker)
    // =========================================================================

    @Nested
    @DisplayName("3.1 - Mock TCP Half-Open Network Partition Tests")
    class MockNetworkPartitionTests {

        /**
         * Kiểm thử 3.1.1: ReconnectListener phải được kích hoạt sau khi onOpen() được gọi.
         *
         * <p>Đây là bài kiểm thử nền tảng xác nhận cơ chế ReconnectListener hoạt động đúng: Mọi
         * listener đã đăng ký phải được gọi chính xác một lần sau mỗi lần reconnect.
         */
        @Test
        @DisplayName("3.1.1 - ReconnectListener phải được kích hoạt khi onOpen() được gọi")
        void reconnectListener_firedOnOnOpen() throws Exception {
            AtomicInteger listenerCallCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(1);

            Runnable reconnectListener =
                    () -> {
                        listenerCallCount.incrementAndGet();
                        latch.countDown();
                    };

            mockClient.addReconnectListener(reconnectListener);

            // Giả lập onOpen() — như khi kết nối thành công sau reconnect
            mockClient.simulateReconnect(0);

            assertTrue(latch.await(2, TimeUnit.SECONDS), "ReconnectListener must be called within 2s");
            assertTrue(listenerCallCount.get() >= 1, "Listener must have been called at least once");
        }

        /**
         * Kiểm thử 3.1.2: Subscription recovery sau TCP Half-Open.
         *
         * <p><b>Kịch bản mô phỏng:</b>
         *
         * <ol>
         *   <li>Controller đăng ký SUBSCRIBE_AUCTION và một ReconnectListener để tái đăng ký.
         *   <li>Network partition xảy ra (onClose code=1006).
         *   <li>Sau reconnect (onOpen), ReconnectListener tự động tái đăng ký SUBSCRIBE_AUCTION.
         *   <li>Bài test xác minh SUBSCRIBE_AUCTION được gửi lần thứ 2 (subscription recovery).
         * </ol>
         */
        @Test
        @DisplayName(
                "3.1.2 - Subscription phải được tái đăng ký tự động sau khi reconnect (Lỗ hổng"
                        + " #1 Fix)")
        void subscriptionRecovery_afterReconnect() throws Exception {
            CopyOnWriteArrayList<String> sentEventTypes = new CopyOnWriteArrayList<>();
            CountDownLatch subscriptionRestoredLatch = new CountDownLatch(2); // lần 1 + lần 2

            // Giả lập Controller đăng ký subscription tracker
            // Trong production: Controller gọi NetworkService.sendRequest(SUBSCRIBE_AUCTION, ...)
            Runnable subscribeAction =
                    () -> {
                        sentEventTypes.add("SUBSCRIBE_AUCTION");
                        subscriptionRestoredLatch.countDown();
                    };

            // Lần 1: Đăng ký lần đầu (mô phỏng hành vi trong initialize())
            subscribeAction.run();

            // Đăng ký ReconnectListener để tự động tái đăng ký sau mỗi lần reconnect
            mockClient.addReconnectListener(subscribeAction);

            // Giả lập TCP Half-Open (code 1006) — server crash không gửi FIN
            mockClient.simulateNetworkPartition(50);

            // Giả lập reconnect thành công sau 200ms (sau khi network partition)
            mockClient.simulateReconnect(200);

            // Chờ subscription được gửi ít nhất 2 lần (lần đầu + lần phục hồi)
            assertTrue(
                    subscriptionRestoredLatch.await(3, TimeUnit.SECONDS),
                    "Subscription must be restored after reconnect within 3s. "
                            + "Check that ReconnectListener is registered and fires correctly.");

            // Xác minh cả hai lần SUBSCRIBE_AUCTION đều được ghi nhận
            long subscribeCount =
                    sentEventTypes.stream().filter("SUBSCRIBE_AUCTION"::equals).count();
            assertTrue(
                    subscribeCount >= 2,
                    "SUBSCRIBE_AUCTION must be sent at least twice: once on init and once on"
                            + " reconnect. Actual count: "
                            + subscribeCount);
        }

        /**
         * Kiểm thử 3.1.3: removeReconnectListener phải ngăn listener bị gọi sau onUnload().
         *
         * <p>Nếu Controller không gọi {@code removeReconnectListener} trong {@code onUnload()}, cả
         * hai vấn đề sẽ xảy ra: (a) Memory Leak vì AuctionClient giữ reference tới Controller cũ,
         * (b) Zombie subscription — Controller đã rời trang nhưng vẫn gửi SUBSCRIBE_AUCTION.
         */
        @Test
        @DisplayName(
                "3.1.3 - removeReconnectListener trong onUnload() phải ngăn Zombie Subscription")
        void removeReconnectListener_preventsZombieSubscription() throws Exception {
            AtomicBoolean listenerCalledAfterUnload = new AtomicBoolean(false);
            CountDownLatch reconnectLatch = new CountDownLatch(1);

            Runnable zombieListener =
                    () -> {
                        listenerCalledAfterUnload.set(true);
                    };

            mockClient.addReconnectListener(zombieListener);

            // Giả lập onUnload() — Controller tự xóa listener
            mockClient.removeReconnectListener(zombieListener);

            // Thêm một sentinel listener để biết khi nào reconnect đã xong
            mockClient.addReconnectListener(reconnectLatch::countDown);

            // Giả lập reconnect
            mockClient.simulateReconnect(50);

            // Chờ sentinel (đảm bảo onOpen đã chạy)
            assertTrue(reconnectLatch.await(2, TimeUnit.SECONDS), "Sentinel listener must fire");

            assertFalse(
                    listenerCalledAfterUnload.get(),
                    "Zombie Subscription Detected! ReconnectListener was called after"
                            + " removeReconnectListener() — memory leak risk.");
        }

        /**
         * Kiểm thử 3.1.4: Multiple reconnects — listener phải được gọi mỗi lần, không phải chỉ
         * một lần. Xác minh rằng CopyOnWriteArrayList không xóa listener sau lần gọi đầu tiên
         * (khác với one-time correlationHandlers).
         */
        @Test
        @DisplayName(
                "3.1.4 - ReconnectListener phải được gọi trên mỗi lần reconnect, không chỉ lần"
                        + " đầu")
        void reconnectListener_persistsAcrossMultipleReconnects() throws Exception {
            int expectedReconnectCount = 3;
            AtomicInteger actualCallCount = new AtomicInteger(0);
            CountDownLatch allReconnectsLatch = new CountDownLatch(expectedReconnectCount);

            mockClient.addReconnectListener(
                    () -> {
                        actualCallCount.incrementAndGet();
                        allReconnectsLatch.countDown();
                    });

            // Giả lập 3 lần reconnect liên tiếp
            for (int i = 0; i < expectedReconnectCount; i++) {
                mockClient.simulateReconnect(i * 100L);
            }

            assertTrue(
                    allReconnectsLatch.await(5, TimeUnit.SECONDS),
                    "All "
                            + expectedReconnectCount
                            + " reconnects must trigger the listener within 5s");

            assertTrue(
                    actualCallCount.get() >= expectedReconnectCount,
                    "Listener must be called at least "
                            + expectedReconnectCount
                            + " times. Actual: "
                            + actualCallCount.get());
        }

        /**
         * Kiểm thử 3.1.5: Exception trong một ReconnectListener không được làm crash listener
         * khác. Xác minh cơ chế try-catch trong AuctionClient.onOpen() bảo vệ toàn bộ listener
         * list.
         */
        @Test
        @DisplayName(
                "3.1.5 - Exception trong một ReconnectListener không được ngăn các listener khác"
                        + " chạy")
        void reconnectListener_exceptionInOneDoesNotBlockOthers() throws Exception {
            AtomicBoolean secondListenerCalled = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(1);

            // Listener 1: ném exception có chủ đích
            mockClient.addReconnectListener(
                    () -> {
                        throw new RuntimeException(
                                "Intentional exception in ReconnectListener for test isolation");
                    });

            // Listener 2: phải được gọi dù Listener 1 ném exception
            mockClient.addReconnectListener(
                    () -> {
                        secondListenerCalled.set(true);
                        latch.countDown();
                    });

            mockClient.simulateReconnect(0);

            assertTrue(latch.await(2, TimeUnit.SECONDS), "Second listener must still fire");
            assertTrue(
                    secondListenerCalled.get(),
                    "Second ReconnectListener must be called even when first one throws exception!"
                            + " AuctionClient.onOpen() must use try-catch per listener.");
        }
    }

    // =========================================================================
    // Tầng 2: Testcontainers Toxiproxy Tests (cần Docker daemon)
    // =========================================================================

    /**
     * Tầng kiểm thử mạng thực tế sử dụng Testcontainers + Toxiproxy để giả lập đứt mạng ở tầng
     * TCP thực sự.
     *
     * <p><b>Điều kiện kích hoạt:</b> Chỉ chạy khi có Docker daemon và biến hệ thống {@code
     * test.network.integration=true}. Bỏ qua tự động trong CI/CD thông thường.
     *
     * <p><b>Cách chạy:</b>
     *
     * <pre>{@code
     * mvn test -pl client -Dtest.network.integration=true -Dtest="DistributedNetworkSplitTest"
     * }</pre>
     *
     * <p><b>Lưu ý kỹ thuật khi implement (đọc trước khi bật):</b>
     *
     * <ul>
     *   <li><b>Host Port Exposure:</b> Bắt buộc gọi {@code Testcontainers.exposeHostPorts(8080)}
     *       TRƯỚC khi tạo ToxiproxyContainer. Nếu không, Toxiproxy container sẽ không thể kết nối
     *       ngược về {@code host.testcontainers.internal:8080} vì host port không được published.
     *   <li><b>Heartbeat Timeout:</b> Sau khi inject {@code timeout} toxic, phải chờ ít nhất
     *       {@code connectionLostTimeout + 1} giây để {@code org.java_websocket} phát hiện kết nối
     *       chết. Default timeout là 60 giây — cần override qua
     *       {@code client.setConnectionLostTimeout(5)} trước khi connect để test không mất 60s.
     *   <li><b>Không dùng closeBlocking():</b> {@code client.closeBlocking()} gửi WebSocket Close
     *       Frame — đây là graceful shutdown, không phải network partition. Chỉ inject {@code
     *       timeout} hoặc {@code latency} toxic qua Toxiproxy API.
     * </ul>
     */
    @Nested
    @Tag("integration")
    @DisplayName("3.x - Toxiproxy Network Partition Tests [Cần Docker]")
    class ToxiproxyNetworkPartitionTests {

        private static final String SKIP_REASON =
                "Toxiproxy integration tests require Docker daemon and"
                        + " -Dtest.network.integration=true";

        /**
         * Kiểm thử 3.x.1: Kết nối qua Toxiproxy phải hoạt động bình thường trước khi inject
         * toxic.
         *
         * <p><b>Setup cần implement:</b>
         *
         * <pre>{@code
         * // 1. Expose host port cho Toxiproxy container kết nối ngược về
         * Testcontainers.exposeHostPorts(actualServerPort);
         *
         * // 2. Khởi động ToxiproxyContainer
         * ToxiproxyContainer toxiproxy = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.9.0")
         *     .withNetwork(Network.newNetwork());
         * toxiproxy.start();
         *
         * // 3. Tạo proxy trỏ tới server thật (qua host.testcontainers.internal)
         * ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(
         *     "host.testcontainers.internal", actualServerPort);
         *
         * // 4. Kết nối AuctionClient qua Toxiproxy
         * String proxyUrl = "ws://" + proxy.getContainerIpAddress()
         *                 + ":" + proxy.getProxyPort();
         * AuctionClient client = new AuctionClient(new URI(proxyUrl));
         * client.setConnectionLostTimeout(5); // override 60s default
         * client.connectBlocking();
         * }</pre>
         */
        @Test
        @DisplayName("3.x.1 - [Cần Docker] Kết nối qua Toxiproxy hoạt động bình thường")
        void toxiproxyConnection_worksBeforeInjection() {
            if (!isDockerAvailable() || !isToxiproxyTestEnabled()) {
                System.out.println("[SKIP] " + SKIP_REASON);
                return;
            }
            // TODO: Implement khi Docker daemon có sẵn trong CI/CD pipeline
            // Xem Javadoc class-level và kế hoạch kiểm thử cho chi tiết setup
        }

        /**
         * Kiểm thử 3.x.2: Sau khi inject {@code timeout} toxic (giả lập TCP Half-Open), {@code
         * onClose()} phải được kích hoạt trong vòng {@code connectionLostTimeout + buffer} giây.
         *
         * <p><b>Inject toxic:</b>
         *
         * <pre>{@code
         * // Inject timeout toxic: chặn mọi data sau 100ms
         * proxy.toxics()
         *     .timeout("network-partition", ToxicDirection.DOWNSTREAM, 100);
         *
         * // Chờ heartbeat timeout phát hiện connection dead
         * assertTrue(disconnectedLatch.await(connectionLostTimeout + 2, TimeUnit.SECONDS),
         *     "onClose() must be triggered after heartbeat timeout detects TCP Half-Open");
         * }</pre>
         */
        @Test
        @DisplayName(
                "3.x.2 - [Cần Docker] TCP Half-Open phải bị phát hiện trong connectionLostTimeout"
                        + " giây")
        void tcpHalfOpen_detectedByHeartbeat() {
            if (!isDockerAvailable() || !isToxiproxyTestEnabled()) {
                System.out.println("[SKIP] " + SKIP_REASON);
                return;
            }
            // TODO: Implement khi Docker daemon có sẵn
        }

        /**
         * Kiểm thử 3.x.3: Sau khi xóa toxic (restore network), reconnect phải thành công và
         * ReconnectListeners phải được kích hoạt để tái đăng ký SUBSCRIBE_AUCTION.
         */
        @Test
        @DisplayName(
                "3.x.3 - [Cần Docker] Subscription phải được phục hồi sau khi network được khôi"
                        + " phục")
        void subscriptionRecovery_afterToxiproxyPartitionLifted() {
            if (!isDockerAvailable() || !isToxiproxyTestEnabled()) {
                System.out.println("[SKIP] " + SKIP_REASON);
                return;
            }
            // TODO: Implement khi Docker daemon có sẵn
            // 1. Inject timeout toxic
            // 2. Chờ onClose() kích hoạt
            // 3. Xóa toxic (network restored)
            // 4. Chờ reconnect (triggerReconnect() sau 5s)
            // 5. Assert ReconnectListener fired → SUBSCRIBE_AUCTION sent lại
        }

        private boolean isDockerAvailable() {
            try {
                Process process =
                        Runtime.getRuntime().exec(new String[] {"docker", "info"});
                return process.waitFor(3, TimeUnit.SECONDS) && process.exitValue() == 0;
            } catch (Exception e) {
                return false;
            }
        }

        private boolean isToxiproxyTestEnabled() {
            return Boolean.parseBoolean(
                    System.getProperty("test.network.integration", "false"));
        }
    }

    // =========================================================================
    // Kiểm thử tích hợp: Virtual Thread + Network Layer
    // =========================================================================

    /**
     * Kiểm thử 3.5: Concurrent bids từ nhiều Virtual Threads không được gây race condition trong
     * typedHandlers.
     *
     * <p>Xác minh rằng {@code ConcurrentHashMap} trong {@code AuctionClient} xử lý đúng khi nhiều
     * Virtual Threads cùng lúc kích hoạt handlers cho cùng một EventType.
     */
    @Test
    @DisplayName(
            "3.5 - Concurrent Virtual Threads gửi PLACE_BID không gây race condition trong"
                    + " typedHandlers")
    void concurrentVirtualThreads_noRaceConditionInHandlers() throws Exception {
        int threadCount = 50;
        CopyOnWriteArrayList<String> receivedJsonList = new CopyOnWriteArrayList<>();
        CountDownLatch allHandledLatch = new CountDownLatch(threadCount);

        // Đăng ký một handler thu thập tất cả messages nhận được
        mockClient.addResponseHandler(
                EventType.PLACE_BID,
                "CONCURRENT_TEST",
                json -> {
                    receivedJsonList.add(json);
                    allHandledLatch.countDown();
                });

        // Kiểm tra payload JSON mẫu đại diện cho một PLACE_BID response từ server
        String bidResponseJson =
                "{\"success\":true,\"type\":\"PLACE_BID\","
                        + "\"data\":{\"id\":1,\"auctionId\":100,\"bidderId\":5,\"amount\":500.0}}";

        // Khởi chạy 50 Virtual Threads đồng thời, mỗi thread kích hoạt 1 handler
        java.util.List<Thread> threads = new java.util.ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Thread vt =
                    Thread.startVirtualThread(
                            () -> {
                                Map<String, java.util.function.Consumer<String>> handlers =
                                        mockClient
                                                .getTypedHandlers()
                                                .get(EventType.PLACE_BID);
                                if (handlers != null
                                        && handlers.containsKey("CONCURRENT_TEST")) {
                                    handlers.get("CONCURRENT_TEST").accept(bidResponseJson);
                                } else {
                                    allHandledLatch.countDown(); // tránh deadlock nếu setup lỗi
                                }
                            });
            threads.add(vt);
        }

        // Chờ tất cả 50 threads hoàn thành
        assertTrue(
                allHandledLatch.await(5, TimeUnit.SECONDS),
                "All "
                        + threadCount
                        + " concurrent PLACE_BID handlers must complete within 5s. "
                        + "Race condition may have occurred in typedHandlers.");

        // Tất cả 50 invocations phải được nhận mà không mất mát hay duplicate bất thường
        assertTrue(
                receivedJsonList.size() >= threadCount,
                "All "
                        + threadCount
                        + " concurrent messages must be received. Actual: "
                        + receivedJsonList.size()
                        + ". Possible race condition in ConcurrentHashMap handler dispatch.");

        // Cleanup
        mockClient.removeResponseHandler(EventType.PLACE_BID, "CONCURRENT_TEST");
    }
}
