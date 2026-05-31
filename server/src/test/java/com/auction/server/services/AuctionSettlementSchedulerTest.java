package com.auction.server.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.users.User;
import com.auction.core.users.UserFactory;
import com.auction.server.dao.DBConnection;
import com.auction.server.dao.impl.IAuctionDao;
import com.auction.server.dao.impl.IBidDao;
import com.auction.server.dao.impl.IUserDao;
import com.auction.server.network.BroadcastBroker;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuctionSettlementSchedulerTest {

    @Mock private IAuctionDao auctionDao;
    @Mock private IBidDao bidDao;
    @Mock private IUserDao userDao;
    @Mock private Connection mockConnection;

    private AuctionSettlementScheduler scheduler;
    private MockedStatic<BroadcastBroker> brokerMock;
    private BroadcastBroker mockBroker;

    private Auction testAuction;
    private User winnerUser;
    private User sellerUser;
    private User loserUser;

    @BeforeEach
    void setUp() throws Exception {
        DBConnection.closeConnection();
        brokerMock = mockStatic(BroadcastBroker.class);
        mockBroker = mock(BroadcastBroker.class);
        brokerMock.when(BroadcastBroker::getInstance).thenReturn(mockBroker);

        scheduler = new AuctionSettlementScheduler(auctionDao, bidDao, userDao);

        testAuction = new Auction();
        testAuction.setId(100);
        testAuction.setStatus(Auction.Status.ACTIVE);
        testAuction.setStartingPrice(100.0);
        testAuction.setEndTime(LocalDateTime.now().minusMinutes(5)); // Hết hạn 5 phút trước

        winnerUser = UserFactory.rehydrateUser(
                "STANDARD", 20, "winner", "hash",
                "Winner", "win@test.com",
                BigDecimal.valueOf(1000.0), BigDecimal.valueOf(30.0), true // Có cọc 30.0
        );

        sellerUser = UserFactory.rehydrateUser(
                "STANDARD", 10, "seller", "hash",
                "Seller", "sell@test.com",
                BigDecimal.valueOf(200.0), BigDecimal.ZERO, true
        );

        loserUser = UserFactory.rehydrateUser(
                "STANDARD", 30, "loser", "hash",
                "Loser", "loser@test.com",
                BigDecimal.valueOf(500.0), BigDecimal.valueOf(30.0), true // Có cọc 30.0
        );

        // Inject mock connection to bypass static thread-local DBConnection restrictions
        injectMockConnection();
    }

    @AfterEach
    void tearDown() throws Exception {
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

    /**
     * Happy Path: Kết toán thành công khi người thắng có đủ số dư thanh toán phần còn lại.
     */
    @Test
    void should_SettleAuctionSuccessfully_when_WinnerHasSufficientBalance() throws Exception {
        // Arrange
        Bid highestBid = new Bid(1, 100, 20, 250.0); // Bid 250.0

        when(auctionDao.getAuctionDetailsForUpdate(any(Connection.class), eq(100)))
                .thenReturn(testAuction);
        when(bidDao.findByAuctionId(any(Connection.class), eq(100)))
                .thenReturn(List.of(highestBid));
        when(auctionDao.getSellerId(any(Connection.class), eq(100)))
                .thenReturn(10); // Seller ID = 10

        // Mock khóa User
        when(userDao.findByIdForUpdate(any(Connection.class), eq(20))).thenReturn(winnerUser);
        when(userDao.findByIdForUpdate(any(Connection.class), eq(10))).thenReturn(sellerUser);

        when(userDao.updateBalanceAndLockedBalance(any(Connection.class), any(User.class)))
                .thenReturn(true);
        when(userDao.insertTransactionRecord(any(), anyInt(), anyString(), any(), anyString(), anyString()))
                .thenReturn(true);
        when(auctionDao.updateAuctionInformation(any(Connection.class), any(Auction.class)))
                .thenReturn(true);

        // Act
        java.lang.reflect.Method method = AuctionSettlementScheduler.class.getDeclaredMethod("settleAuction", Integer.class);
        method.setAccessible(true);
        method.invoke(scheduler, 100);

        // Assert
        // StartingPrice = 100.0 -> Deposit = 30.0
        // Total Win Amount = 250.0. Remaining = 250 - 30 = 220.0
        // Winner balance ban đầu: 1000.0 -> Đóng nốt 220.0, cọc 30.0 bị khấu trừ -> Số dư cuối: 1000 - 220 = 780.0
        assertEquals(BigDecimal.valueOf(780.0), winnerUser.getBalance());
        assertEquals(0, winnerUser.getLockedBalance().compareTo(BigDecimal.ZERO));

        // Seller balance ban đầu: 200.0 -> Cộng full 250.0 -> Số dư cuối: 450.0
        assertEquals(BigDecimal.valueOf(450.0), sellerUser.getBalance());

        // Trạng thái đấu giá
        assertEquals(Auction.Status.ENDED, testAuction.getStatus());
        assertEquals(20, testAuction.getWinnerId());
        assertEquals(250.0, testAuction.getFinalPrice());

        verify(mockConnection, times(1)).commit();
    }

    /**
     * Negative/Default Path: Winner bùng tiền (không đủ số dư thanh toán phần còn lại).
     * Phạt 30% cọc chuyển cho Seller.
     */
    @Test
    void should_PenalizeWinnerAndCancelAuction_when_WinnerDefaulted() throws Exception {
        // Arrange
        // Winner chỉ còn 100.0 balance.
        winnerUser = UserFactory.rehydrateUser(
                "STANDARD", 20, "winner", "hash",
                "Winner", "win@test.com",
                BigDecimal.valueOf(100.0), BigDecimal.valueOf(30.0), true
        );

        Bid highestBid = new Bid(1, 100, 20, 250.0); // Bid 250.0

        when(auctionDao.getAuctionDetailsForUpdate(any(Connection.class), eq(100)))
                .thenReturn(testAuction);
        when(bidDao.findByAuctionId(any(Connection.class), eq(100)))
                .thenReturn(List.of(highestBid));
        when(auctionDao.getSellerId(any(Connection.class), eq(100)))
                .thenReturn(10); // Seller ID = 10

        when(userDao.findByIdForUpdate(any(Connection.class), eq(20))).thenReturn(winnerUser);
        when(userDao.findByIdForUpdate(any(Connection.class), eq(10))).thenReturn(sellerUser);

        when(userDao.updateBalanceAndLockedBalance(any(Connection.class), any(User.class)))
                .thenReturn(true);
        when(userDao.insertTransactionRecord(any(), anyInt(), anyString(), any(), anyString(), anyString()))
                .thenReturn(true);
        when(auctionDao.updateAuctionInformation(any(Connection.class), any(Auction.class)))
                .thenReturn(true);

        // Act
        java.lang.reflect.Method method = AuctionSettlementScheduler.class.getDeclaredMethod("settleAuction", Integer.class);
        method.setAccessible(true);
        method.invoke(scheduler, 100);

        // Assert
        // Winner bị phạt tịch thu cọc 30.0. Số dư còn: 100.0, Locked balance về 0
        assertEquals(BigDecimal.valueOf(100.0), winnerUser.getBalance());
        assertEquals(0, winnerUser.getLockedBalance().compareTo(BigDecimal.ZERO));

        // Seller được cộng phạt 30.0 -> Số dư cuối: 200 + 30 = 230.0
        assertEquals(BigDecimal.valueOf(230.0), sellerUser.getBalance());

        // Trạng thái đấu giá hủy
        assertEquals(Auction.Status.CANCELLED, testAuction.getStatus());
        assertEquals(0.0, testAuction.getFinalPrice());

        verify(mockConnection, times(1)).commit();
    }

    /**
     * Concurrency & Batching Path: Hoàn trả cọc bất đồng bộ cho người thua thầu (Losers).
     */
    @Test
    void should_RefundLosersAsynchronouslyInBatches_when_SettlingAuction() throws Exception {
        // Arrange
        // Winner ID: 20, Loser ID: 30
        Bid winBid = new Bid(1, 100, 20, 250.0);
        Bid loseBid = new Bid(2, 100, 30, 200.0);

        when(auctionDao.getAuctionDetailsForUpdate(any(Connection.class), eq(100)))
                .thenReturn(testAuction);
        when(bidDao.findByAuctionId(any(Connection.class), eq(100)))
                .thenReturn(Arrays.asList(winBid, loseBid));
        when(auctionDao.getSellerId(any(Connection.class), eq(100)))
                .thenReturn(10);

        lenient().when(userDao.findByIdForUpdate(any(Connection.class), eq(20))).thenReturn(winnerUser);
        lenient().when(userDao.findByIdForUpdate(any(Connection.class), eq(10))).thenReturn(sellerUser);

        // Khóa & trả về loser trong batch refund
        lenient().when(userDao.findByIdForUpdate(any(Connection.class), eq(30))).thenReturn(loserUser);
        lenient().when(userDao.updateBalanceAndLockedBalance(any(Connection.class), any(User.class)))
                .thenReturn(true);

        lenient().when(userDao.insertTransactionRecord(
                any(), anyInt(), anyString(), any(), anyString(), anyString()))
                .thenReturn(true);

        CountDownLatch refundLatch = new CountDownLatch(1);
        lenient().when(userDao.insertTransactionRecord(
                any(), eq(30), eq("DEPOSIT"), any(), eq("SUCCESS"), eq("BID_REFUND_100")))
                .thenAnswer(invocation -> {
                    refundLatch.countDown();
                    return true;
                });

        // Act
        java.lang.reflect.Method method = AuctionSettlementScheduler.class.getDeclaredMethod("settleAuction", Integer.class);
        method.setAccessible(true);
        method.invoke(scheduler, 100);

        // Chờ tác vụ hoàn cọc bất đồng bộ hoàn thành
        boolean refundCompleted = refundLatch.await(3, TimeUnit.SECONDS);
        assertTrue(refundCompleted, "Tác vụ hoàn cọc bất đồng bộ cho Loser không được kích hoạt hoặc quá chậm.");

        // Assert
        // Loser được trả lại cọc 30.0: Balance ban đầu 500.0, cọc 30.0 đóng băng.
        // Hậu hoàn cọc: Balance = 530.0, Locked balance = 0.
        assertEquals(BigDecimal.valueOf(530.0), loserUser.getBalance());
        assertEquals(0, loserUser.getLockedBalance().compareTo(BigDecimal.ZERO));
    }

    /**
     * Concurrency & Deadlock Prevention: Đảm bảo thứ tự khóa Winner và Seller luôn tăng dần theo ID.
     */
    @Test
    void should_PreventDeadlock_when_LockingWinnerAndSellerConcurrent() throws Exception {
        // Arrange
        // Winner ID: 20, Seller ID: 10
        // Thứ tự khóa đúng là: 10 trước, 20 sau.
        Bid highestBid = new Bid(1, 100, 20, 250.0);

        when(auctionDao.getAuctionDetailsForUpdate(any(Connection.class), eq(100)))
                .thenReturn(testAuction);
        when(bidDao.findByAuctionId(any(Connection.class), eq(100)))
                .thenReturn(List.of(highestBid));
        when(auctionDao.getSellerId(any(Connection.class), eq(100)))
                .thenReturn(10);

        when(userDao.findByIdForUpdate(any(Connection.class), eq(20))).thenReturn(winnerUser);
        when(userDao.findByIdForUpdate(any(Connection.class), eq(10))).thenReturn(sellerUser);

        // Act
        java.lang.reflect.Method method = AuctionSettlementScheduler.class.getDeclaredMethod("settleAuction", Integer.class);
        method.setAccessible(true);
        method.invoke(scheduler, 100);

        // Assert
        // Xác minh thứ tự gọi khóa FOR UPDATE
        InOrder lockOrderVerifier = inOrder(userDao);
        lockOrderVerifier.verify(userDao).findByIdForUpdate(any(Connection.class), eq(10)); // Khóa Seller ID=10 trước
        lockOrderVerifier.verify(userDao).findByIdForUpdate(any(Connection.class), eq(20)); // Khóa Winner ID=20 sau
    }
}
