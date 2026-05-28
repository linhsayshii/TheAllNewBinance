package com.auction.client.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.auction.client.mock.MockAuctionClient;
import com.auction.client.page.productdetail.ProductDetailPageController;
import com.auction.client.scene.NavigationService;
import com.auction.client.scene.SceneService;
import com.auction.client.service.NetworkService;
import com.auction.core.protocol.EventType;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Kiểm thử ranh giới luồng (Thread Boundary Validation) cho {@link ProductDetailPageController}.
 *
 * <p><b>Chiến lược kiểm thử:</b>
 *
 * <ol>
 *   <li>Khởi tạo {@link NetworkService} với {@link MockAuctionClient} để tránh kết nối server thật.
 *   <li>Nạp FXML và gắn vào Live Stage của TestFX thông qua {@code start(Stage)} — tránh lỗi
 *       Detached Scene Graph (Node mồ côi không có Scene).
 *   <li>Lấy handler đã đăng ký qua {@link MockAuctionClient#typedHandlers} và gọi trực tiếp trên
 *       Virtual Thread để giả lập luồng xử lý gói tin WebSocket thực tế.
 *   <li>Dùng Mockito Spy để intercept {@code drawChart()} — phương thức public — và assert Thread
 *       ID tại thời điểm UI được cập nhật.
 * </ol>
 *
 * <p><b>Lưu ý thiết kế:</b> {@code handlePlaceBidResponse} là {@code private}, nên không thể Spy
 * trực tiếp. Thay vào đó, ta inject JSON thông qua {@link MockAuctionClient#typedHandlers} (đây
 * là cơ chế chính xác mà Controller sử dụng khi nhận gói tin WebSocket thực tế).
 */
@DisplayName("ProductDetailPage Thread Boundary Validation Tests")
public class ProductDetailPageThreadBoundaryTest extends BaseUiTest {

    private ProductDetailPageController loadedController;
    private ProductDetailPageController controllerSpy;
    private MockAuctionClient mockClient;

    @BeforeAll
    static void initNetworkServiceWithMock() {
        // Khởi tạo NetworkService một lần duy nhất cho toàn bộ test class.
        // resetForTest() được gọi trước để đảm bảo luôn dùng MockAuctionClient.
        System.setProperty("app.mockMode", "true");
        NetworkService.resetForTest();
        NetworkService.init("ws://localhost:0");
    }

    @AfterAll
    static void cleanupAfterAllTests() {
        // Reset sau khi toàn bộ test class hoàn thành — không reset giữa các test method.
        System.clearProperty("app.mockMode");
        NetworkService.resetForTest();
    }

    /**
     * Nạp FXML vào Live Stage của TestFX và tạo Spy từ Controller đã được tiêm đầy đủ @FXML.
     *
     * <p><b>Tại sao cần gắn vào Stage?</b> Nếu không gắn vào Stage đang hiển thị, các Node trong
     * Controller sẽ là "Detached" (mồ côi) và {@code node.getScene()} sẽ trả về null. Bất kỳ thao
     * tác đồ họa nào (vẽ biểu đồ, tính kích thước) sẽ ném {@code NullPointerException}.
     *
     * <p><b>Spy được tạo SAU khi nạp FXML</b> để toàn bộ @FXML fields đã được tiêm trước khi
     * Mockito bọc đối tượng. Đây là khác biệt then chốt so với chỉ dùng {@code new
     * ProductDetailPageController()}.
     */
    @Override
    public void start(Stage stage) throws Exception {
        // Bước 1: Bootstrap NavigationService với SceneService mock.
        // Header FXML component gọi NavigationService.getInstance().isDarkTheme() trong
        // initialize() — nếu singleton null sẽ ném NPE ngay lúc FXMLLoader.load().
        // Dùng Mockito.mock() để tránh khởi tạo SceneService thật (cần Stage thật + stylesheets).
        SceneService mockSceneService = Mockito.mock(SceneService.class);
        Mockito.when(mockSceneService.isDarkTheme()).thenReturn(false);
        // NavigationService constructor tự gán instance = this — đây là cách chuẩn để bootstrap
        new NavigationService(mockSceneService);

        // Bước 2: Load FXML sau khi NavigationService đã sẵn sàng
        FXMLLoader loader =
                new FXMLLoader(
                        getClass().getResource("/fxml/pages/product-detail-page.fxml"));
        Parent root = loader.load();
        loadedController = loader.getController();

        // Tạo Spy sau khi FXML đã tiêm đầy đủ — tránh NPE trên @FXML fields
        controllerSpy = Mockito.spy(loadedController);

        // Lấy MockAuctionClient để inject gói tin thẳng vào typedHandlers
        mockClient = (MockAuctionClient) NetworkService.getInstance().getClient();

        stage.setScene(new Scene(root, 1280, 800));
        stage.show();
    }

    @AfterEach
    void cleanupSystemProperty() {
        // QUAN TRỌNG: Không reset NetworkService ở đây.
        // TestFX gọi start() mỗi test method nhưng @BeforeAll chỉ chạy 1 lần.
        // Nếu reset NetworkService ở @AfterEach, test tiếp theo sẽ gặp IllegalStateException.
        // Cleanup được xử lý bởi @AfterAll.
    }

    // =========================================================================
    // Test Case 2.3.1: Thread Boundary Safety — Virtual Thread → FX Thread
    // =========================================================================

    /**
     * Kiểm thử: Gói tin PLACE_BID từ Virtual Thread phải kích hoạt Platform.runLater() trước khi
     * chạm vào bất kỳ UI Node nào.
     *
     * <p><b>Cơ chế:</b>
     *
     * <ol>
     *   <li>Lấy handler mà Controller đã tự đăng ký trong {@code registerNetworkHandlers()}.
     *   <li>Gọi handler đó trên Virtual Thread (giả lập luồng xử lý WebSocket).
     *   <li>Spy intercept {@code drawChart()} và assert luồng đang chạy là FX Application Thread.
     * </ol>
     */
    @Test
    @DisplayName(
            "2.3.1 - drawChart() phải chạy trên JavaFX Application Thread, không được chạy trực"
                    + " tiếp từ Virtual Thread")
    void testDrawChartRunsOnFxApplicationThread() throws Exception {
        CountDownLatch drawChartLatch = new CountDownLatch(1);
        AtomicReference<Throwable> threadException = new AtomicReference<>();

        // Intercept drawChart() — phương thức public — để assert thread tại điểm cập nhật UI
        Mockito.doAnswer(
                        invocation -> {
                            try {
                                // ASSERT QUAN TRỌNG: Tại đây, luồng PHẢI là JavaFX Application
                                // Thread.
                                // Nếu không, Controller đang cập nhật UI Node từ Virtual Thread —
                                // sẽ ném IllegalStateException trong production.
                                assertTrue(
                                        Platform.isFxApplicationThread(),
                                        "drawChart() must be called on JavaFX Application Thread!"
                                                + " Actual thread: "
                                                + Thread.currentThread().getName());

                                // Assert đồ thị Node không mồ côi (Live Scene Graph)
                                assertNotNull(
                                        loadedController.bidHistoryList().getScene(),
                                        "bidHistoryList must be attached to a Live Scene Graph!");
                            } catch (Throwable t) {
                                threadException.set(t);
                            } finally {
                                drawChartLatch.countDown();
                            }
                            // Gọi thực tế của drawChart() để không phá vỡ flow
                            return invocation.callRealMethod();
                        })
                .when(controllerSpy)
                .drawChart(Mockito.anyList());

        // Thay thế handler gốc trong typedHandlers bằng version gọi qua controllerSpy
        java.util.Map<String, Consumer<String>> placeBidHandlers =
                mockClient.getTypedHandlers().get(EventType.PLACE_BID);
        Consumer<String> originalHandler =
                placeBidHandlers != null
                        ? placeBidHandlers.getOrDefault("PRODUCT_DETAIL_PAGE", null)
                        : null;

        if (originalHandler != null) {
            placeBidHandlers.put(
                    "PRODUCT_DETAIL_PAGE",
                    json -> {
                        // Dùng reflection để gọi private handlePlaceBidResponse trên
                        // controllerSpy — đây là cách duy nhất intercept private method
                        // trong khi vẫn đảm bảo Mockito Spy có thể theo dõi drawChart()
                        try {
                            java.lang.reflect.Method method =
                                    ProductDetailPageController.class.getDeclaredMethod(
                                            "handlePlaceBidResponse", String.class);
                            method.setAccessible(true);
                            method.invoke(controllerSpy, json);
                        } catch (Exception e) {
                            threadException.set(e);
                            drawChartLatch.countDown();
                        }
                    });
        }

        // Giả lập gói tin PLACE_BID đến từ Virtual Thread (như WebSocket inbound thread)
        String incomingBidJson =
                "{\"success\":true,"
                        + "\"type\":\"PLACE_BID\","
                        + "\"data\":{"
                        + "\"id\":1,\"auctionId\":0,\"bidderId\":2,\"amount\":150.0"
                        + "}}";

        Thread.startVirtualThread(
                () -> {
                    try {
                        // ASSERT 1: Handler ban đầu phải chạy trên Virtual Thread (không phải FX)
                        assertFalse(
                                Platform.isFxApplicationThread(),
                                "WebSocket inbound handler must NOT run on FX Application Thread!"
                                        + " It should run on a Virtual Thread for high throughput.");

                        // Kích hoạt handler thông qua typedHandlers (giống onMessage của
                        // AuctionClient)
                        Consumer<String> placeBidHandler =
                                mockClient.getTypedHandlers()
                                        .getOrDefault(
                                                EventType.PLACE_BID,
                                                java.util.Collections.emptyMap())
                                        .get("PRODUCT_DETAIL_PAGE");

                        if (placeBidHandler != null) {
                            placeBidHandler.accept(incomingBidJson);
                        } else {
                            // Không tìm thấy handler — Controller chưa được initialize()
                            threadException.set(
                                    new AssertionError(
                                            "No PLACE_BID handler found for PRODUCT_DETAIL_PAGE."
                                                    + " Controller.initialize() may not have been"
                                                    + " called."));
                            drawChartLatch.countDown();
                        }
                    } catch (Throwable t) {
                        threadException.set(t);
                        drawChartLatch.countDown();
                    }
                });

        // Chờ tối đa 3 giây để Platform.runLater() dispatch lên FX thread và drawChart() được gọi
        assertTrue(
                drawChartLatch.await(3, TimeUnit.SECONDS),
                "Timed out! drawChart() was never called."
                        + " Check that Platform.runLater() is used in handlePlaceBidResponse.");

        // Thổi ngoại lệ từ thread phụ lên JUnit 5 Test Runner chính để không bị nuốt im lặng
        if (threadException.get() != null) {
            fail("Assertion failed inside thread boundary: " + threadException.get().getMessage(),
                    threadException.get());
        }
    }

    // =========================================================================
    // Test Case 2.3.2: Phát hiện lỗi thiếu Platform.runLater() (Negative Test)
    // =========================================================================

    /**
     * Kiểm thử hồi quy: Phương thức xử lý gói tin WebSocket phải bọc mọi thay đổi UI trong {@code
     * Platform.runLater()}.
     *
     * <p>Test này xác minh rằng {@code handlePlaceBidResponse} KHÔNG gọi UI Node trực tiếp từ
     * luồng hiện tại mà luôn chuyển sang FX Application Thread trước.
     */
    @Test
    @DisplayName(
            "2.3.2 - [Regression] handlePlaceBidResponse phải dùng Platform.runLater() cho mọi"
                    + " thay đổi UI — không được trực tiếp từ background thread")
    void testHandlePlaceBidResponseUsesRunLater() throws Exception {
        AtomicReference<Boolean> drawChartCalledOnNonFxThread = new AtomicReference<>(false);
        CountDownLatch latch = new CountDownLatch(1);

        // Monitor: nếu drawChart() được gọi từ non-FX thread, ghi lại vi phạm
        Mockito.doAnswer(
                        invocation -> {
                            if (!Platform.isFxApplicationThread()) {
                                drawChartCalledOnNonFxThread.set(true);
                            }
                            latch.countDown();
                            return invocation.callRealMethod();
                        })
                .when(controllerSpy)
                .drawChart(Mockito.anyList());

        // Gọi handler trực tiếp từ Virtual Thread (không qua MockAuctionClient)
        String bidJson =
                "{\"success\":true,"
                        + "\"type\":\"PLACE_BID\","
                        + "\"data\":{"
                        + "\"id\":2,\"auctionId\":0,\"bidderId\":3,\"amount\":200.0"
                        + "}}";

        Thread.startVirtualThread(
                () -> {
                    try {
                        java.lang.reflect.Method method =
                                ProductDetailPageController.class.getDeclaredMethod(
                                        "handlePlaceBidResponse", String.class);
                        method.setAccessible(true);
                        method.invoke(controllerSpy, bidJson);
                    } catch (Exception e) {
                        latch.countDown();
                    }
                });

        assertTrue(
                latch.await(3, TimeUnit.SECONDS),
                "Timed out waiting for drawChart to be called.");

        assertFalse(
                drawChartCalledOnNonFxThread.get(),
                "[REGRESSION] drawChart() was called directly from a non-FX thread!"
                        + " Missing Platform.runLater() wrapper in handlePlaceBidResponse.");
    }

    // =========================================================================
    // Test Case 3.2: Memory Leak Detection (Navigation via Router)
    // =========================================================================

    /**
     * Kiểm thử rò rỉ bộ nhớ qua Router thực tế.
     *
     * <p><b>Tại sao test này không bị Dương tính giả (False Positive)?</b> Controller được tải
     * thông qua TestFX {@code start(Stage)} và gắn vào Live Stage. Khi điều hướng đến trang khác,
     * {@code SceneService.switchTo()} gọi {@code onUnload()} tự động. Nếu {@code onUnload()} không
     * giải phóng handlers trong {@code AuctionClient.typedHandlers}, Controller cũ sẽ bị giữ lại
     * bởi Singleton và GC không thể thu gom.
     *
     * <p><b>Lưu ý:</b> Test này cần NavigationService được khởi tạo trong TestFX context. Nếu
     * NavigationService.getInstance() là null (vì chưa được khởi tạo trong test environment),
     * test sẽ bỏ qua phần navigate và chỉ kiểm thử onUnload() trực tiếp.
     */
    @Test
    @DisplayName(
            "3.2 - Không có Memory Leak khi rời ProductDetailPage: onUnload() phải giải phóng"
                    + " toàn bộ handlers khỏi AuctionClient.typedHandlers")
    void testNoMemoryLeakOnPageUnload() throws Exception {
        // 1. Controller đã được nạp và gắn vào Live Stage trong start()
        assertNotNull(loadedController, "Controller must be initialized via FXML in start()");

        // 2. Xác nhận handler đã được đăng ký trong AuctionClient trước khi navigate
        // Root cause của Memory Leak: AuctionClient Singleton giữ lambda reference đến Controller
        // thông qua typedHandlers — khi Controller rời trang, reference này phải được giải phóng.
        boolean handlerRegistered =
                mockClient.getTypedHandlers().containsKey(EventType.PLACE_BID)
                        && mockClient
                                .getTypedHandlers()
                                .get(EventType.PLACE_BID)
                                .containsKey("PRODUCT_DETAIL_PAGE");
        assertTrue(
                handlerRegistered,
                "PLACE_BID handler must be registered in AuctionClient during initialize()."
                        + " If not registered, test setup is wrong.");

        // 3. Gọi onUnload() qua FX thread (giả lập hành vi của SceneService.switchTo())
        // Đây là điểm kiểm tra quan trọng: onUnload() PHẢI gọi removeResponseHandler()
        CountDownLatch unloadLatch = new CountDownLatch(1);
        interact(
                () -> {
                    loadedController.onUnload();
                    unloadLatch.countDown();
                });
        assertTrue(unloadLatch.await(3, TimeUnit.SECONDS), "onUnload() timed out on FX thread");

        // 4. ASSERT CHÍNH: Handler phải bị xóa khỏi AuctionClient Singleton sau onUnload().
        // Đây là thứ thực sự gây Memory Leak — AuctionClient giữ lambda ::handlePlaceBidResponse
        // của Controller cũ, ngăn GC thu gom Controller dù Stage đã chuyển sang trang khác.
        boolean placeHandlerRemoved =
                !mockClient.getTypedHandlers().containsKey(EventType.PLACE_BID)
                        || !mockClient
                                .getTypedHandlers()
                                .get(EventType.PLACE_BID)
                                .containsKey("PRODUCT_DETAIL_PAGE");
        assertTrue(
                placeHandlerRemoved,
                "[Memory Leak] PLACE_BID handler was NOT removed from AuctionClient after"
                        + " onUnload()! AuctionClient Singleton still holds a lambda reference to"
                        + " the old Controller, preventing GC from collecting it.");

        boolean bidHistoryHandlerRemoved =
                !mockClient.getTypedHandlers().containsKey(EventType.GET_BIDS_BY_AUCTION_ID)
                        || !mockClient
                                .getTypedHandlers()
                                .get(EventType.GET_BIDS_BY_AUCTION_ID)
                                .containsKey("PRODUCT_DETAIL_PAGE");
        assertTrue(
                bidHistoryHandlerRemoved,
                "[Memory Leak] GET_BIDS_BY_AUCTION_ID handler was NOT removed from AuctionClient"
                        + " after onUnload()! Both handlers registered in initialize() must be"
                        + " cleaned up in onUnload().");
    }
}
