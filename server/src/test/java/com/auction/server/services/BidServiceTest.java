package com.auction.server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.dto.auction.AuctionDetailsDto;
import com.auction.core.dto.bid.PlaceBid;
import com.auction.core.exception.ErrorCode;
import com.auction.core.exception.auction.AuctionClosedException;
import com.auction.core.exception.auction.InvalidBidException;
import com.auction.core.exception.auction.ShillBiddingForbiddenException;
import com.auction.core.exception.wallet.InsufficientBalanceException;
import com.auction.core.services.IAuctionService;
import com.auction.core.users.User;
import com.auction.server.dao.impl.IBidDao;
import com.auction.server.dao.impl.IUserDao;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BidServiceTest {

    @Mock private IBidDao bidDao;

    @Mock private IAuctionService auctionService;

    @Mock private IUserDao userDao;

    @Mock private BidQueueManager bidQueueManager;

    @InjectMocks private BidService bidService;

    private User testUser;
    private Auction testAuction;
    private PlaceBid placeBidRequest;

    @BeforeEach
    void setUp() {
        testUser =
                com.auction.core.users.UserFactory.rehydrateUser(
                        "STANDARD",
                        2,
                        "bidder",
                        "pass",
                        "Bidder User",
                        "bidder@test.com",
                        java.math.BigDecimal.valueOf(1000.0),
                        java.math.BigDecimal.ZERO,
                        true);
        testAuction = new Auction();
        testAuction.setId(1);
        testAuction.setStatus(Auction.Status.ACTIVE);
        testAuction.setStartingPrice(100.0);
        testAuction.setStartTime(LocalDateTime.now().minusDays(1));
        testAuction.setEndTime(LocalDateTime.now().plusDays(1));

        placeBidRequest = new PlaceBid();
        placeBidRequest.setAuctionId(1);
        placeBidRequest.setBidderId(2);
        placeBidRequest.setAmount(150.0);
    }

    @Test
    void testPlaceBid_ValidBid() {
        CompletableFuture<AuctionDetailsDto> auctionFuture =
                CompletableFuture.completedFuture(new AuctionDetailsDto(testAuction, null, null));
        CompletableFuture<Integer> sellerFuture = CompletableFuture.completedFuture(1);

        when(userDao.findById(2)).thenReturn(testUser);
        when(auctionService.getAuctionDetails(1)).thenReturn(auctionFuture);
        when(auctionService.getSellerId(1)).thenReturn(sellerFuture); // Seller is 1, bidder is 2
        when(bidDao.hasBid(1, 2)).thenReturn(false);
        when(userDao.holdBalance(2, 30.0)).thenReturn(true); // 30% of 100

        Bid expectedBid = new Bid(1, 1, 2, 150.0);
        CompletableFuture<Bid> expectedBidFuture = CompletableFuture.completedFuture(expectedBid);
        when(bidQueueManager.submitBid(any(BidTask.class))).thenReturn(expectedBidFuture);

        CompletableFuture<Bid> resultFuture = bidService.placeBid(placeBidRequest);

        Bid result = resultFuture.join();
        assertNotNull(result);
        assertEquals(150.0, result.getAmount());
        verify(bidQueueManager, times(1)).submitBid(any(BidTask.class));
        verify(userDao, times(1)).holdBalance(2, 30.0);
    }

    @Test
    void testPlaceBid_AuctionEnded_ShouldThrowAuctionClosedException() {
        testAuction.setEndTime(LocalDateTime.now().minusHours(1)); // Auction ended
        CompletableFuture<AuctionDetailsDto> auctionFuture =
                CompletableFuture.completedFuture(new AuctionDetailsDto(testAuction, null, null));
        CompletableFuture<Integer> sellerFuture = CompletableFuture.completedFuture(1);

        when(userDao.findById(2)).thenReturn(testUser);
        when(auctionService.getAuctionDetails(1)).thenReturn(auctionFuture);
        when(auctionService.getSellerId(1)).thenReturn(sellerFuture);

        CompletableFuture<Bid> resultFuture = bidService.placeBid(placeBidRequest);

        CompletionException exception = assertThrows(CompletionException.class, resultFuture::join);
        assertTrue(exception.getCause() instanceof AuctionClosedException);
        assertEquals(
                ErrorCode.AUCTION_CLOSED,
                ((AuctionClosedException) exception.getCause()).getErrorCode());
        verify(bidQueueManager, never()).submitBid(any(BidTask.class));
    }

    @Test
    void testPlaceBid_AuctionNotActive_ShouldThrowAuctionClosedException() {
        testAuction.setStatus(Auction.Status.PENDING);
        CompletableFuture<AuctionDetailsDto> auctionFuture =
                CompletableFuture.completedFuture(new AuctionDetailsDto(testAuction, null, null));

        when(userDao.findById(2)).thenReturn(testUser);
        when(auctionService.getAuctionDetails(1)).thenReturn(auctionFuture);

        CompletableFuture<Bid> resultFuture = bidService.placeBid(placeBidRequest);

        CompletionException exception = assertThrows(CompletionException.class, resultFuture::join);
        assertTrue(exception.getCause() instanceof AuctionClosedException);
        assertEquals(
                ErrorCode.AUCTION_CLOSED,
                ((AuctionClosedException) exception.getCause()).getErrorCode());
        verify(bidQueueManager, never()).submitBid(any(BidTask.class));
    }

    @Test
    void testPlaceBid_InsufficientBalance_ShouldThrowInsufficientBalanceException() {
        CompletableFuture<AuctionDetailsDto> auctionFuture =
                CompletableFuture.completedFuture(new AuctionDetailsDto(testAuction, null, null));
        CompletableFuture<Integer> sellerFuture = CompletableFuture.completedFuture(1);

        when(userDao.findById(2)).thenReturn(testUser);
        when(auctionService.getAuctionDetails(1)).thenReturn(auctionFuture);
        when(auctionService.getSellerId(1)).thenReturn(sellerFuture);
        when(bidDao.hasBid(1, 2)).thenReturn(false);
        when(userDao.holdBalance(2, 30.0)).thenReturn(false); // Insufficient balance

        CompletableFuture<Bid> resultFuture = bidService.placeBid(placeBidRequest);

        CompletionException exception = assertThrows(CompletionException.class, resultFuture::join);
        assertTrue(exception.getCause() instanceof InsufficientBalanceException);
        assertEquals(
                ErrorCode.INSUFFICIENT_BALANCE,
                ((InsufficientBalanceException) exception.getCause()).getErrorCode());
        verify(bidQueueManager, never()).submitBid(any(BidTask.class));
    }

    @Test
    void testPlaceBid_ShillBidding_ShouldThrowShillBiddingForbiddenException() {
        placeBidRequest.setBidderId(1); // Bidder is the same as seller
        testUser =
                com.auction.core.users.UserFactory.rehydrateUser(
                        "STANDARD",
                        1,
                        "bidder",
                        "pass",
                        "Bidder User",
                        "bidder@test.com",
                        java.math.BigDecimal.valueOf(1000.0),
                        java.math.BigDecimal.ZERO,
                        true);
        CompletableFuture<AuctionDetailsDto> auctionFuture =
                CompletableFuture.completedFuture(new AuctionDetailsDto(testAuction, null, null));
        CompletableFuture<Integer> sellerFuture = CompletableFuture.completedFuture(1);

        when(userDao.findById(1)).thenReturn(testUser);
        when(auctionService.getAuctionDetails(1)).thenReturn(auctionFuture);
        when(auctionService.getSellerId(1)).thenReturn(sellerFuture);

        CompletableFuture<Bid> resultFuture = bidService.placeBid(placeBidRequest);

        CompletionException exception = assertThrows(CompletionException.class, resultFuture::join);
        assertTrue(exception.getCause() instanceof ShillBiddingForbiddenException);
        assertEquals(
                ErrorCode.SHILL_BIDDING_FORBIDDEN,
                ((ShillBiddingForbiddenException) exception.getCause()).getErrorCode());
        verify(bidQueueManager, never()).submitBid(any(BidTask.class));
    }

    @Test
    void testPlaceBid_InvalidAmount_ShouldThrowInvalidBidException() {
        placeBidRequest.setAmount(0.0);
        CompletableFuture<AuctionDetailsDto> auctionFuture =
                CompletableFuture.completedFuture(new AuctionDetailsDto(testAuction, null, null));
        CompletableFuture<Integer> sellerFuture = CompletableFuture.completedFuture(1);

        when(userDao.findById(2)).thenReturn(testUser);
        when(auctionService.getAuctionDetails(1)).thenReturn(auctionFuture);
        when(auctionService.getSellerId(1)).thenReturn(sellerFuture);

        CompletableFuture<Bid> resultFuture = bidService.placeBid(placeBidRequest);

        CompletionException exception = assertThrows(CompletionException.class, resultFuture::join);
        assertTrue(exception.getCause() instanceof InvalidBidException);
        assertEquals(
                ErrorCode.INVALID_BID_AMOUNT,
                ((InvalidBidException) exception.getCause()).getErrorCode());
        verify(bidQueueManager, never()).submitBid(any(BidTask.class));
    }
}
