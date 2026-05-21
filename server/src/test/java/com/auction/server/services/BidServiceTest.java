package com.auction.server.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.dto.bid.PlaceBid;
import com.auction.core.services.IAuctionService;
import com.auction.core.users.User;
import com.auction.core.dto.auction.AuctionDetailsDto;
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
                new User(
                        2,
                        "bidder",
                        "pass",
                        "Bidder User",
                        "bidder@test.com",
                        1000.0,
                        User.Role.STANDARD,
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
    void testPlaceBid_InvalidBid_AuctionEnded() {
        testAuction.setEndTime(LocalDateTime.now().minusHours(1)); // Auction ended
        CompletableFuture<AuctionDetailsDto> auctionFuture =
                CompletableFuture.completedFuture(new AuctionDetailsDto(testAuction, null, null));
        CompletableFuture<Integer> sellerFuture = CompletableFuture.completedFuture(1);

        when(userDao.findById(2)).thenReturn(testUser);
        when(auctionService.getAuctionDetails(1)).thenReturn(auctionFuture);
        when(auctionService.getSellerId(1)).thenReturn(sellerFuture);

        CompletableFuture<Bid> resultFuture = bidService.placeBid(placeBidRequest);

        CompletionException exception = assertThrows(CompletionException.class, resultFuture::join);
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertEquals("Auction has already ended", exception.getCause().getMessage());

        verify(bidQueueManager, never()).submitBid(any(BidTask.class));
    }

    @Test
    void testPlaceBid_InvalidBid_InsufficientBalanceForDeposit() {
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
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertEquals("Insufficient balance for deposit", exception.getCause().getMessage());

        verify(bidQueueManager, never()).submitBid(any(BidTask.class));
    }

    @Test
    void testPlaceBid_InvalidBid_ShillBidding() {
        placeBidRequest.setBidderId(1); // Bidder is the same as seller
        testUser.setId(1);
        CompletableFuture<AuctionDetailsDto> auctionFuture =
                CompletableFuture.completedFuture(new AuctionDetailsDto(testAuction, null, null));
        CompletableFuture<Integer> sellerFuture = CompletableFuture.completedFuture(1);

        when(userDao.findById(1)).thenReturn(testUser);
        when(auctionService.getAuctionDetails(1)).thenReturn(auctionFuture);
        when(auctionService.getSellerId(1)).thenReturn(sellerFuture);

        CompletableFuture<Bid> resultFuture = bidService.placeBid(placeBidRequest);

        CompletionException exception = assertThrows(CompletionException.class, resultFuture::join);
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertEquals("Seller cannot bid on their own auction", exception.getCause().getMessage());

        verify(bidQueueManager, never()).submitBid(any(BidTask.class));
    }

    @Test
    void testPlaceBid_InvalidBid_AmountLessOrEqualZero() {
        placeBidRequest.setAmount(0.0);
        CompletableFuture<AuctionDetailsDto> auctionFuture =
                CompletableFuture.completedFuture(new AuctionDetailsDto(testAuction, null, null));
        CompletableFuture<Integer> sellerFuture = CompletableFuture.completedFuture(1);

        when(userDao.findById(2)).thenReturn(testUser);
        when(auctionService.getAuctionDetails(1)).thenReturn(auctionFuture);
        when(auctionService.getSellerId(1)).thenReturn(sellerFuture);

        CompletableFuture<Bid> resultFuture = bidService.placeBid(placeBidRequest);

        CompletionException exception = assertThrows(CompletionException.class, resultFuture::join);
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertEquals("Invalid bid amount", exception.getCause().getMessage());

        verify(bidQueueManager, never()).submitBid(any(BidTask.class));
    }

    @Test
    void testPlaceBid_InvalidBid_AuctionNotActive() {
        testAuction.setStatus(Auction.Status.PENDING);
        CompletableFuture<AuctionDetailsDto> auctionFuture =
                CompletableFuture.completedFuture(new AuctionDetailsDto(testAuction, null, null));

        when(userDao.findById(2)).thenReturn(testUser);
        when(auctionService.getAuctionDetails(1)).thenReturn(auctionFuture);

        CompletableFuture<Bid> resultFuture = bidService.placeBid(placeBidRequest);

        CompletionException exception = assertThrows(CompletionException.class, resultFuture::join);
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertEquals("Auction is not active", exception.getCause().getMessage());

        verify(bidQueueManager, never()).submitBid(any(BidTask.class));
    }
}
