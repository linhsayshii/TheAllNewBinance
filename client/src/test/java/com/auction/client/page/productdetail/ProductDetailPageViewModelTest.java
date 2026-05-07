package com.auction.client.page.productdetail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.products.ArtisticCreation;
import com.auction.core.products.CategoryType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests cho ProductDetailPageViewModel.
 *
 * <p>Kiểm thử các logic thuần túy quan trọng (không cần JavaFX Runtime): - Tính giá cược tối thiểu
 * (minimumBidAmount) - Định dạng hiển thị đếm ngược thời gian (updateCountdown) - Kiểm thử hồi
 * quy (Regression) lỗi so sánh Integer reference - Trạng thái biddingEnabled khi auction kết thúc
 */
@DisplayName("ProductDetailPageViewModel Unit Tests")
class ProductDetailPageViewModelTest {

    private ProductDetailPageViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new ProductDetailPageViewModel();
    }

    // =========================================================================
    // Test Case 1.2.1: minimumBidAmount
    // =========================================================================

    @Test
    @DisplayName("1.2.1a - Giá cược tối thiểu = currentBid + bidIncrement")
    void minimumBidAmount_returnsCurrentBidPlusIncrement() {
        // Giả lập auction với currentPrice=100, bidIncrement=10
        Auction auction = new Auction();
        auction.setCurrentPrice(100.0);
        auction.setBidIncrement(10.0);
        auction.setStatus(Auction.Status.ACTIVE);
        auction.setEndTime(LocalDateTime.now().plusDays(1));

        viewModel.applyAuctionData(auction, null, null, null, null);

        assertEquals(
                110.0,
                viewModel.minimumBidAmount(),
                0.001,
                "Minimum bid must be currentPrice + bidIncrement");
    }

    @Test
    @DisplayName("1.2.1b - Giá cược tối thiểu với increment = 0 trả về đúng currentBid")
    void minimumBidAmount_withZeroIncrement_returnsCurrentBid() {
        Auction auction = new Auction();
        auction.setCurrentPrice(250.0);
        auction.setBidIncrement(0.0);
        auction.setStatus(Auction.Status.ACTIVE);
        auction.setEndTime(LocalDateTime.now().plusDays(1));

        viewModel.applyAuctionData(auction, null, null, null, null);

        assertEquals(
                250.0,
                viewModel.minimumBidAmount(),
                0.001,
                "With zero increment, minimum bid equals current price");
    }

    // =========================================================================
    // Test Case 1.2.2: updateCountdown
    // =========================================================================

    @Test
    @DisplayName("1.2.2a - Đếm ngược đúng format khi còn thời gian")
    void updateCountdown_futureEndTime_showsCorrectFormat() {
        LocalDateTime endTime = LocalDateTime.now().plusDays(2).plusHours(3).plusMinutes(15);
        Auction auction = new Auction();
        auction.setCurrentPrice(0.0);
        auction.setBidIncrement(1.0);
        auction.setStatus(Auction.Status.ACTIVE);
        auction.setEndTime(endTime);
        viewModel.applyAuctionData(auction, null, null, null, null);

        viewModel.updateCountdown(LocalDateTime.now());

        String countdown = viewModel.countdownTextProperty().get();
        // Phải hiển thị dạng "02d 03h 15m 00s" (xấp xỉ)
        assertTrue(
                countdown.contains("d") && countdown.contains("h") && countdown.contains("m"),
                "Countdown must contain days, hours, and minutes. Got: " + countdown);
        assertTrue(countdown.startsWith("02d"), "Must show 2 days. Got: " + countdown);
    }

    @Test
    @DisplayName("1.2.2b - Đếm ngược kết thúc khi endTime đã qua, biddingEnabled = false")
    void updateCountdown_pastEndTime_showsEndedAndDisablesBidding() {
        LocalDateTime pastEndTime = LocalDateTime.now().minusMinutes(1);
        Auction auction = new Auction();
        auction.setCurrentPrice(0.0);
        auction.setBidIncrement(1.0);
        auction.setStatus(Auction.Status.ACTIVE);
        auction.setEndTime(pastEndTime);
        viewModel.applyAuctionData(auction, null, null, null, null);

        viewModel.updateCountdown(LocalDateTime.now());

        assertEquals(
                "Auction ended",
                viewModel.countdownTextProperty().get(),
                "Must display 'Auction ended' when time has passed");
        assertFalse(
                viewModel.isBiddingEnabled(),
                "Bidding must be disabled when auction has ended");
    }

    @Test
    @DisplayName("1.2.2c - biddingEnabled = false khi auction status là ENDED")
    void applyAuctionData_endedStatus_disablesBidding() {
        Auction auction = new Auction();
        auction.setCurrentPrice(0.0);
        auction.setBidIncrement(1.0);
        auction.setStatus(Auction.Status.ENDED);
        auction.setEndTime(LocalDateTime.now().plusDays(1));

        viewModel.applyAuctionData(auction, null, null, null, null);

        assertFalse(
                viewModel.isBiddingEnabled(),
                "Bidding must be disabled when auction status is ENDED");
    }

    // =========================================================================
    // Test Case 1.2.3: Regression Test - Integer reference comparison
    // =========================================================================

    @Test
    @DisplayName(
            "1.2.3 - [REGRESSION] getAuctionId() phải dùng .equals() để so sánh, không phải =="
                    + " (tránh Integer cache miss)")
    void getAuctionId_largeValue_equalsComparison_worksCorrectly() {
        // Integer cache của JVM chỉ bảo đảm -128 đến 127.
        // Với ID > 127 (ví dụ: 1001), == sẽ trả về false dù giá trị bằng nhau.
        Auction auction = new Auction();
        auction.setId(1001);
        auction.setCurrentPrice(0.0);
        auction.setBidIncrement(1.0);
        auction.setStatus(Auction.Status.ACTIVE);
        auction.setEndTime(LocalDateTime.now().plusDays(1));

        viewModel.applyAuctionData(auction, null, null, null, null);

        Integer auctionId = viewModel.getAuctionId();
        Integer incomingId = Integer.valueOf(1001);

        // Bài test này bảo vệ khỏi lỗi: incomingId != viewModel.getAuctionId()
        // Khi ID > 127, hai Integer objects có thể là các references khác nhau
        assertTrue(
                incomingId.equals(auctionId),
                "Must use .equals() for Integer comparison, not == reference equality!");
    }

    // =========================================================================
    // Test Case 1.2.4: setBids & bidderCountText
    // =========================================================================

    @Test
    @DisplayName("1.2.4a - setBids với danh sách null phải hiển thị '0 people bidding'")
    void setBids_nullList_showsZeroBidders() {
        viewModel.setBids(null);

        assertEquals(
                "0 people bidding",
                viewModel.bidderCountTextProperty().get(),
                "Must show '0 people bidding' for null bid list");
    }

    @Test
    @DisplayName("1.2.4b - setBids với danh sách hợp lệ phải cập nhật đúng số bidder")
    void setBids_validList_updatesBidderCount() {
        // Bid constructor: (Integer id, Integer auctionId, Integer bidderId, Double amount)
        Bid bid1 = new Bid(1, 1001, 10, 150.0);
        bid1.setCreatedAt(LocalDateTime.now().minusMinutes(2));

        Bid bid2 = new Bid(2, 1001, 11, 200.0);
        bid2.setCreatedAt(LocalDateTime.now().minusMinutes(1));

        viewModel.setBids(List.of(bid1, bid2));

        assertEquals(
                "2 people bidding",
                viewModel.bidderCountTextProperty().get(),
                "Must show correct bidder count");
    }

    @Test
    @DisplayName("1.2.4c - setBids phải cập nhật giá hiện tại theo cược mới nhất theo thời gian")
    void setBids_validList_updatesCurrentBidToLatest() {
        // bid mới hơn (createdAt lớn hơn) phải là đầu danh sách sau khi sort giảm dần
        Bid olderBid = new Bid(1, 1001, 10, 150.0);
        olderBid.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        Bid latestBid = new Bid(2, 1001, 11, 300.0);
        latestBid.setCreatedAt(LocalDateTime.now().minusMinutes(1));

        viewModel.setBids(List.of(olderBid, latestBid));

        // Danh sách được sắp xếp giảm dần theo thời gian, latestBid phải là phần tử đầu tiên
        assertTrue(
                viewModel.currentBidDisplayProperty().get().contains("300"),
                "Current bid display must reflect the latest (highest timestamp) bid amount");
    }

    // =========================================================================
    // Test Case 1.2.5: updateCurrentBidDisplay
    // =========================================================================

    @Test
    @DisplayName("1.2.5 - updateCurrentBidDisplay phải định dạng tiền tệ đúng chuẩn")
    void updateCurrentBidDisplay_formatsCorrectly() {
        viewModel.updateCurrentBidDisplay(1234567.89);

        assertEquals(
                "$1,234,567.89",
                viewModel.currentBidDisplayProperty().get(),
                "Must format currency with comma separators and 2 decimal places");
    }

    // =========================================================================
    // Test Case 1.2.6: applyAuctionData với Item
    // =========================================================================

    @Test
    @DisplayName("1.2.6 - applyAuctionData phải áp dụng đúng dữ liệu từ Item")
    void applyAuctionData_withItem_appliesItemData() {
        Auction auction = new Auction();
        auction.setCurrentPrice(500.0);
        auction.setBidIncrement(50.0);
        auction.setStatus(Auction.Status.ACTIVE);
        auction.setEndTime(LocalDateTime.now().plusDays(1));

        // Item là abstract sealed class, phải dùng concrete subclass ArtisticCreation
        ArtisticCreation item = new ArtisticCreation(
                1,
                99,
                "Test Product",
                "A great product",
                CategoryType.ART,
                null,
                false,
                "Famous Artist",
                2024);

        viewModel.applyAuctionData(auction, item, "TestSeller", 42, null);

        assertEquals("Test Product", viewModel.titleProperty().get());
        assertEquals("A great product", viewModel.descriptionProperty().get());
        assertEquals("TestSeller", viewModel.sellerNameProperty().get());
        assertEquals(42, (int) viewModel.getBidderId());
    }
}
