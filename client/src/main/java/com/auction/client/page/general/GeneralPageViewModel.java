package com.auction.client.page.general;

import java.util.List;

import com.auction.client.dto.ProductCardUiModel;
import com.auction.client.service.AuctionQueryService;

public class GeneralPageViewModel {

    private final AuctionQueryService auctionQueryService;

    public GeneralPageViewModel() {
        this(new AuctionQueryService());
    }

    public GeneralPageViewModel(AuctionQueryService auctionQueryService) {
        this.auctionQueryService = auctionQueryService;
    }

    public List<ProductCardUiModel> loadFeaturedAuctions() {
        return auctionQueryService.getFeaturedAuctions();
    }

    public List<ProductCardUiModel> loadLiveFeaturedAuctions() {
        return auctionQueryService.getFeaturedAuctionFeed().liveAuctions();
    }

    public List<ProductCardUiModel> loadUpcomingFeaturedAuctions() {
        return auctionQueryService.getFeaturedAuctionFeed().upcomingAuctions();
    }
}