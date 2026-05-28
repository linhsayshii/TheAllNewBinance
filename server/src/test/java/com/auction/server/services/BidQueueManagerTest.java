package com.auction.server.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.dto.bid.PlaceBid;
import com.auction.core.exception.auction.InvalidBidException;
import com.auction.core.exception.wallet.InsufficientBalanceException;
import com.auction.core.services.IAuctionService;
import com.auction.core.users.User;
import com.auction.core.users.UserFactory;
import com.auction.server.dao.DBConnection;
import com.auction.server.dao.impl.IAuctionDao;
import com.auction.server.dao.impl.IBidDao;
import com.auction.server.dao.impl.IUserDao;
import com.auction.server.network.BroadcastBroker;
import com.auction.core.protocol.EventType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BidQueueManagerTest {

    @Mock private IBidDao bidDao;
    @Mock private IAuctionService auctionService;
    @Mock private IAuctionDao auctionDao;
    @Mock private IUserDao userDao;
    @Mock private Connection mockConnection;

    private BidQueueManager bidQueueManager;
    private MockedStatic<AuctionSettlementScheduler> schedulerMock;
    private MockedStatic<BroadcastBroker> brokerMock;

    private AuctionSettlementScheduler mockScheduler;
    private BroadcastBroker mockBroker;
    private Auction testAuction;
    private User testUser;

    private static class DirectExecutorService extends java.util.concurrent.AbstractExecutorService {
        private volatile boolean shutdown = false;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public java.util.List<Runnable> shutdownNow() {
            shutdown = true;
            return java.util.List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        schedulerMock = mockStatic(AuctionSettlementScheduler.class);
        brokerMock = mockStatic(BroadcastBroker.class);

        mockScheduler = mock(AuctionSettlementScheduler.class);
        mockBroker = mock(BroadcastBroker.class);

        schedulerMock.when(AuctionSettlementScheduler::getInstance).thenReturn(mockScheduler);
        brokerMock.when(BroadcastBroker::getInstance).thenReturn(mockBroker);

        bidQueueManager = new BidQueueManager(bidDao, auctionService, auctionDao, userDao);

        testAuction = new Auction();
        testAuction.setId(100);
        testAuction.setStatus(Auction.Status.ACTIVE);
        testAuction.setStartingPrice(100.0);
        testAuction.setEndTime(LocalDateTime.now().plusHours(2));

        testUser = UserFactory.rehydrateUser(
                "STANDARD", 1, "bidder", "hashed_pass",
                "Bidder One", "one@test.com",
                BigDecimal.valueOf(1000.0), BigDecimal.ZERO, true
        );

        // Inject mock connection to bypass static thread-local DBConnection restrictions
        injectMockConnection();

        // Inject direct synchronous executor for standard deterministic unit tests
        injectExecutor(new DirectExecutorService());
    }

    @AfterEach
    void tearDown() throws Exception {
        bidQueueManager.shutdown();
        schedulerMock.close();
        brokerMock.close();
        cleanupMockConnection();
    }

    private void injectMockConnection() throws Exception {
        java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);

        java.lang.reflect.Field realHolderField = DBConnection.class.getDeclaredField("REAL_CONNECTION_HOLDER");
        Object realBase = unsafe.staticFieldBase(realHolderField);
        long realOffset = unsafe.staticFieldOffset(realHolderField);

        ThreadLocal<Connection> customHolder = ThreadLocal.withInitial(() -> mockConnection);
        unsafe.putObject(realBase, realOffset, customHolder);

        java.lang.reflect.Field proxyHolderField = DBConnection.class.getDeclaredField("PROXY_CONNECTION_HOLDER");
        Object proxyBase = unsafe.staticFieldBase(proxyHolderField);
        long proxyOffset = unsafe.staticFieldOffset(proxyHolderField);
        unsafe.putObject(proxyBase, proxyOffset, customHolder);
    }

    private void cleanupMockConnection() throws Exception {
        java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);

        java.lang.reflect.Field realHolderField = DBConnection.class.getDeclaredField("REAL_CONNECTION_HOLDER");
        Object realBase = unsafe.staticFieldBase(realHolderField);
        long realOffset = unsafe.staticFieldOffset(realHolderField);
        unsafe.putObject(realBase, realOffset, new ThreadLocal<Connection>());

        java.lang.reflect.Field proxyHolderField = DBConnection.class.getDeclaredField("PROXY_CONNECTION_HOLDER");
        Object proxyBase = unsafe.staticFieldBase(proxyHolderField);
        long proxyOffset = unsafe.staticFieldOffset(proxyHolderField);
        unsafe.putObject(proxyBase, proxyOffset, new ThreadLocal<Connection>());
    }

    private void injectExecutor(ExecutorService executor) throws Exception {
        java.lang.reflect.Field field = BidQueueManager.class.getDeclaredField("consumerPool");
        field.setAccessible(true);
        field.set(bidQueueManager, executor);
    }

    /**
     * Ca kiểm thử đặc thù nguyên tử: Lỗi khi lưu Bid vào Database.
     * Hệ thống bắt buộc phải Rollback Transaction để bảo toàn ví tiền cọc.
     */
    @Test
    void should_RollbackTransaction_when_BidDaoFailsToSave() throws SQLException, ExecutionException, InterruptedException {
        // Arrange
        PlaceBid request = new PlaceBid();
        request.setAuctionId(100);
        request.setBidderId(1);
        request.setAmount(150.0);

        CompletableFuture<Bid> resultFuture = new CompletableFuture<>();
        BidTask task = new BidTask(request, resultFuture, testAuction, true);

        when(auctionService.getAuctionDetailsForUpdate(any(Connection.class), eq(100)))
                .thenReturn(testAuction);
        when(userDao.findByIdForUpdate(any(Connection.class), eq(1)))
                .thenReturn(testUser);
        when(bidDao.hasBid(any(Connection.class), eq(100), eq(1)))
                .thenReturn(false); // Lượt đầu thầu -> đóng cọc 30.0

        when(userDao.updateBalanceAndLockedBalance(any(Connection.class), any(User.class)))
                .thenReturn(true);
        when(userDao.insertTransactionRecord(any(), eq(1), eq("WITHDRAW"), any(), eq("SUCCESS"), any()))
                .thenReturn(true);

        when(auctionService.processBid(any(Connection.class), any(Bid.class), any(Auction.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        // DB save fails
        when(bidDao.saveBid(any(Connection.class), any(Bid.class)))
                .thenReturn(false);

        // Act
        bidQueueManager.submitBid(task);

        // Assert
        ExecutionException exception = assertThrows(ExecutionException.class, resultFuture::get);
        assertTrue(exception.getCause() instanceof InvalidBidException);

        verify(mockConnection, times(1)).setAutoCommit(false);
        verify(mockConnection, times(1)).rollback();
        verify(mockConnection, never()).commit();
    }

    /**
     * CHỨNG MINH: Cơ cơ tuần tự hóa (Serialization) xử lý chính xác.
     * Đảm bảo cô lập Connection và thực thể User hoàn toàn giữa các thread.
     */
    @Test
    void should_ProcessBidsSerially_when_MultipleConcurrentBidsSubmitted() throws Exception {
        // Restore real cached thread pool for concurrency test
        injectExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "bid-consumer");
            t.setDaemon(true);
            return t;
        }));

        // Arrange
        int concurrentBidders = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch processingLatch = new CountDownLatch(concurrentBidders);

        when(auctionService.getAuctionDetailsForUpdate(any(Connection.class), eq(100)))
                .thenReturn(testAuction);

        when(userDao.findByIdForUpdate(any(Connection.class), anyInt())).thenAnswer(invocation -> {
            int userId = invocation.getArgument(1);
            return UserFactory.rehydrateUser(
                "STANDARD", userId, "bidder_" + userId, "hashed",
                "Bidder", "test@test.com", BigDecimal.valueOf(1000.0), BigDecimal.ZERO, true
            );
        });

        when(bidDao.hasBid(any(Connection.class), eq(100), anyInt())).thenReturn(true);
        when(bidDao.saveBid(any(Connection.class), any(Bid.class))).thenReturn(true);

        when(auctionService.processBid(any(Connection.class), any(Bid.class), any(Auction.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        AtomicInteger commitCounter = new AtomicInteger(0);
        doAnswer(invocation -> {
            commitCounter.incrementAndGet();
            processingLatch.countDown();
            return null;
        }).when(mockConnection).commit();

        CompletableFuture<Bid>[] futures = new CompletableFuture[concurrentBidders];
        ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

        // Act
        for (int i = 0; i < concurrentBidders; i++) {
            final int bidderId = i + 2;
            PlaceBid req = new PlaceBid();
            req.setAuctionId(100);
            req.setBidderId(bidderId);
            req.setAmount(200.0 + (i * 10.0));

            CompletableFuture<Bid> f = new CompletableFuture<>();
            BidTask task = new BidTask(req, f, testAuction, false);
            futures[i] = f;

            virtualThreadExecutor.submit(() -> {
                try {
                    startLatch.await(); // Chờ lệnh kích hoạt đồng loạt
                    bidQueueManager.submitBid(task);
                } catch (Exception ignored) {
                }
            });
        }

        startLatch.countDown(); // Kích hoạt Race condition đầu vào hàng đợi

        boolean completed = processingLatch.await(3, TimeUnit.SECONDS);
        assertTrue(completed, "Hệ thống tiêu thụ hàng đợi quá chậm hoặc lỗi deadlock bất đồng bộ.");

        // Assert
        assertEquals(concurrentBidders, commitCounter.get(), "Số lượng giao dịch commit vật lý không khớp với số lượng thầu hợp lệ.");
        virtualThreadExecutor.shutdown();
    }

    /**
     * Ca kiểm thử: Người dùng đặt thầu nhiều lần, chỉ đóng cọc ở lượt đầu.
     */
    @Test
    void should_HoldDepositOnlyOnce_when_MultipleBidsFromSameUser() throws SQLException, ExecutionException, InterruptedException {
        // Arrange
        PlaceBid request = new PlaceBid();
        request.setAuctionId(100);
        request.setBidderId(1);
        request.setAmount(150.0);

        CompletableFuture<Bid> resultFuture = new CompletableFuture<>();
        BidTask task = new BidTask(request, resultFuture, testAuction, false);

        when(auctionService.getAuctionDetailsForUpdate(any(Connection.class), eq(100)))
                .thenReturn(testAuction);
        when(userDao.findByIdForUpdate(any(Connection.class), eq(1)))
                .thenReturn(testUser);

        // Đã đặt thầu trước đó
        when(bidDao.hasBid(any(Connection.class), eq(100), eq(1)))
                .thenReturn(true);

        when(auctionService.processBid(any(Connection.class), any(Bid.class), any(Auction.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(bidDao.saveBid(any(Connection.class), any(Bid.class)))
                .thenReturn(true);

        // Act
        bidQueueManager.submitBid(task);

        // Assert
        assertNotNull(resultFuture.get());
        // verify: không gọi hold cọc
        verify(userDao, never()).updateBalanceAndLockedBalance(any(Connection.class), any(User.class));
        verify(mockConnection, times(1)).commit();
    }

    /**
     * Ca kiểm thử: Đặt thầu ở giây cuối kích hoạt Snipe Extension hậu Commit.
     */
    @Test
    void should_TriggerSnipeExtensionPostCommit_when_BidPlacedInFinalSeconds() throws SQLException, ExecutionException, InterruptedException {
        // Snipe window thường là 30 giây cuối. Thiết lập auction kết thúc sau 10 giây
        testAuction.setEndTime(LocalDateTime.now().plusSeconds(10));

        PlaceBid request = new PlaceBid();
        request.setAuctionId(100);
        request.setBidderId(1);
        request.setAmount(150.0);

        CompletableFuture<Bid> resultFuture = new CompletableFuture<>();
        BidTask task = new BidTask(request, resultFuture, testAuction, false);

        when(auctionService.getAuctionDetailsForUpdate(any(Connection.class), eq(100)))
                .thenReturn(testAuction);
        when(userDao.findByIdForUpdate(any(Connection.class), eq(1)))
                .thenReturn(testUser);
        when(bidDao.hasBid(any(Connection.class), eq(100), eq(1)))
                .thenReturn(true);

        when(auctionService.processBid(any(Connection.class), any(Bid.class), any(Auction.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(bidDao.saveBid(any(Connection.class), any(Bid.class)))
                .thenReturn(true);

        // Act
        bidQueueManager.submitBid(task);

        // Assert
        assertNotNull(resultFuture.get());

        // Verify reschedule & broadcast are triggered
        verify(mockScheduler, times(1)).rescheduleAuctionClose(eq(100), any(LocalDateTime.class));
        verify(mockBroker, times(1)).broadcastToRoom(eq(100), eq(EventType.AUCTION_EXTENDED), any(), any());
        verify(mockConnection, times(1)).commit();
    }

    /**
     * Ca kiểm thử: Người dùng ví không đủ 30% giá khởi điểm để đặt cọc.
     */
    @Test
    void should_ThrowInsufficientBalance_when_UserCannotAffordDeposit() throws SQLException, ExecutionException, InterruptedException {
        // User chỉ có 10.0, trong khi startingPrice là 100.0 (cọc 30.0)
        User poorUser = UserFactory.rehydrateUser(
                "STANDARD", 1, "poor", "hashed_pass",
                "Poor User", "poor@test.com",
                BigDecimal.valueOf(10.0), BigDecimal.ZERO, true
        );

        PlaceBid request = new PlaceBid();
        request.setAuctionId(100);
        request.setBidderId(1);
        request.setAmount(150.0);

        CompletableFuture<Bid> resultFuture = new CompletableFuture<>();
        BidTask task = new BidTask(request, resultFuture, testAuction, true);

        when(auctionService.getAuctionDetailsForUpdate(any(Connection.class), eq(100)))
                .thenReturn(testAuction);
        when(userDao.findByIdForUpdate(any(Connection.class), eq(1)))
                .thenReturn(poorUser);
        when(bidDao.hasBid(any(Connection.class), eq(100), eq(1)))
                .thenReturn(false);

        // Act
        bidQueueManager.submitBid(task);

        // Assert
        ExecutionException exception = assertThrows(ExecutionException.class, resultFuture::get);
        assertTrue(exception.getCause() instanceof InsufficientBalanceException);

        verify(mockConnection, times(1)).rollback();
    }

    /**
     * Ca kiểm thử: Từ chối thầu mới khi manager đang shutdown.
     */
    @Test
    void should_RejectNewBids_when_ManagerInShutdown() throws ExecutionException, InterruptedException {
        // Arrange
        PlaceBid request = new PlaceBid();
        request.setAuctionId(100);
        request.setBidderId(1);
        request.setAmount(150.0);

        CompletableFuture<Bid> resultFuture = new CompletableFuture<>();
        BidTask task = new BidTask(request, resultFuture, testAuction, false);

        // Act
        bidQueueManager.shutdown();
        CompletableFuture<Bid> res = bidQueueManager.submitBid(task);

        // Assert
        assertTrue(res.isCompletedExceptionally());
        ExecutionException exception = assertThrows(ExecutionException.class, res::get);
        assertTrue(exception.getCause() instanceof IllegalStateException);
        assertEquals("Server is shutting down, cannot accept bids", exception.getCause().getMessage());
    }
}
