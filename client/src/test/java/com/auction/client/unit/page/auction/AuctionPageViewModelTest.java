package com.auction.client.unit.page.auction;

import com.auction.client.page.auction.AuctionPageViewModel;
import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.products.Item;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AuctionPageViewModelTest {

    @Test
    void shouldPreserveBidsWhenApplyAuctionDataHasNullHistory() {
        AuctionPageViewModel viewModel = new AuctionPageViewModel();

        // 1. Setup mock bids
        List<Bid> initialBids = new ArrayList<>();
        Bid b1 = new Bid(1, 1001, 2, 100.0);
        b1.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        initialBids.add(b1);

        viewModel.setBids(initialBids);
        Assertions.assertEquals(1, viewModel.bids().size());

        // 2. Apply auction details with NULL bid history
        Auction mockAuction = new Auction();
        mockAuction.setId(1001);
        mockAuction.setStatus(Auction.Status.ACTIVE);
        mockAuction.setCurrentPrice(100.0);
        mockAuction.setStartTime(LocalDateTime.now().minusHours(1));
        mockAuction.setEndTime(LocalDateTime.now().plusHours(1));

        com.auction.core.dto.auction.LuxuryCollectiblePayload attrs =
                new com.auction.core.dto.auction.LuxuryCollectiblePayload();
        attrs.setBrand("Rolex");
        attrs.setCondition("Mint");
        attrs.setHasCertificate(true);

        com.auction.core.products.factory.ItemFactoryProvider.initialize();
        Item mockItem =
                com.auction.core.products.factory.ItemFactoryProvider.getFactory(
                                com.auction.core.products.CategoryType.WATCHES)
                        .createItem(
                                101, 1, "Test Product", "Description", "ImageUrl", false, attrs);

        com.auction.core.users.User mockUser =
                com.auction.core.users.UserFactory.createNewStandard(
                        "seller_username", "password", "Seller", "seller@example.com");

        viewModel.applyAuctionData(mockAuction, mockItem, mockUser, null, null);

        // 3. Bid history MUST be preserved (not cleared)
        Assertions.assertEquals(1, viewModel.bids().size());
        Assertions.assertEquals(100.0, viewModel.bids().get(0).getAmount());
    }

    @Test
    void shouldTransitionPendingToActiveOnCountdown() {
        AuctionPageViewModel viewModel = new AuctionPageViewModel();
        LocalDateTime now = LocalDateTime.now();

        Auction mockAuction = new Auction();
        mockAuction.setId(1002);
        mockAuction.setStatus(Auction.Status.PENDING);
        mockAuction.setStartTime(now.plusSeconds(2)); // starts in 2s
        mockAuction.setEndTime(now.plusHours(2));

        com.auction.core.users.User mockUser =
                com.auction.core.users.UserFactory.createNewStandard(
                        "seller_username", "password", "Seller", "seller@example.com");

        viewModel.applyAuctionData(mockAuction, null, mockUser, null, null);
        Assertions.assertEquals(Auction.Status.PENDING, viewModel.statusProperty().get());
        Assertions.assertFalse(viewModel.biddingEnabledProperty().get());

        // Tick 1s: still pending
        viewModel.updateCountdown(now.plusSeconds(1));
        Assertions.assertEquals(Auction.Status.PENDING, viewModel.statusProperty().get());
        Assertions.assertFalse(viewModel.biddingEnabledProperty().get());

        // Tick 2s: should transition to ACTIVE and enable bidding
        viewModel.updateCountdown(now.plusSeconds(2));
        Assertions.assertEquals(Auction.Status.ACTIVE, viewModel.statusProperty().get());
        Assertions.assertTrue(viewModel.biddingEnabledProperty().get());
    }

    @Test
    void shouldTransitionActiveToEndedOnCountdown() {
        AuctionPageViewModel viewModel = new AuctionPageViewModel();
        LocalDateTime now = LocalDateTime.now();

        Auction mockAuction = new Auction();
        mockAuction.setId(1003);
        mockAuction.setStatus(Auction.Status.ACTIVE);
        mockAuction.setStartTime(now.minusHours(1));
        mockAuction.setEndTime(now.plusSeconds(2)); // ends in 2s

        com.auction.core.users.User mockUser =
                com.auction.core.users.UserFactory.createNewStandard(
                        "seller_username", "password", "Seller", "seller@example.com");

        viewModel.applyAuctionData(mockAuction, null, mockUser, null, null);
        Assertions.assertEquals(Auction.Status.ACTIVE, viewModel.statusProperty().get());
        Assertions.assertTrue(viewModel.biddingEnabledProperty().get());

        // Tick 1s: still active
        viewModel.updateCountdown(now.plusSeconds(1));
        Assertions.assertEquals(Auction.Status.ACTIVE, viewModel.statusProperty().get());
        Assertions.assertTrue(viewModel.biddingEnabledProperty().get());

        // Tick 2s: should transition to ENDED and disable bidding
        viewModel.updateCountdown(now.plusSeconds(2));
        Assertions.assertEquals(Auction.Status.ENDED, viewModel.statusProperty().get());
        Assertions.assertFalse(viewModel.biddingEnabledProperty().get());
    }
}
