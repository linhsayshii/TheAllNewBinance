package com.auction.core.auction;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuctionTest {
    private Auction auction;
    private LocalDateTime originalEndTime;

    @BeforeEach
    void setUp() {
        auction = new Auction();
        originalEndTime = LocalDateTime.of(2026, 5, 28, 10, 0, 0);
        auction.setEndTime(originalEndTime);
    }

    @Test
    @DisplayName("Should extend auction end time if bid is placed within threshold (120s)")
    void testApplySnipeExtension_WithinThreshold() {
        LocalDateTime bidTime = originalEndTime.minusSeconds(60);

        boolean isExtended = auction.applySnipeExtension(bidTime);

        assertThat(isExtended).isTrue();
        assertThat(auction.getEndTime())
                .isEqualTo(bidTime.plusSeconds(auction.getSnipeExtension()));
    }

    @Test
    @DisplayName("Should NOT extend if bid is placed outside threshold (>120s)")
    void testApplySnipeExtension_OutsideThreshold() {
        LocalDateTime bidTime = originalEndTime.minusMinutes(5);

        boolean isExtended = auction.applySnipeExtension(bidTime);

        assertThat(isExtended).isFalse();
        assertThat(auction.getEndTime()).isEqualTo(originalEndTime);
    }

    @Test
    @DisplayName("Cascading Extension: Should handle multiple consecutive extensions correctly")
    void testApplySnipeExtension_Cascading() {
        // Bid 1: 09:59:30 (Trong 120s cuối)
        LocalDateTime bid1Time = originalEndTime.minusSeconds(30);
        boolean extended1 = auction.applySnipeExtension(bid1Time);

        assertThat(extended1).isTrue();
        LocalDateTime newEndTime1 = bid1Time.plusSeconds(120); // 10:01:30
        assertThat(auction.getEndTime()).isEqualTo(newEndTime1);

        // Bid 2: 10:01:00 (Trong 120s cuối của EndTime mới)
        LocalDateTime bid2Time = newEndTime1.minusSeconds(30);
        boolean extended2 = auction.applySnipeExtension(bid2Time);

        assertThat(extended2).isTrue();
        LocalDateTime expectedFinalTime = bid2Time.plusSeconds(120); // 10:03:00
        assertThat(auction.getEndTime()).isEqualTo(expectedFinalTime);
    }

    @Test
    @DisplayName("Should NOT extend if bid time is after end time")
    void testApplySnipeExtension_BidAfterEnd() {
        LocalDateTime bidTime = originalEndTime.plusMinutes(1);

        boolean isExtended = auction.applySnipeExtension(bidTime);

        assertThat(isExtended).isFalse();
        assertThat(auction.getEndTime()).isEqualTo(originalEndTime);
    }
}
