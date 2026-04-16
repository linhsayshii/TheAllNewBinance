package com.auction.client.page.general;

import com.auction.client.dto.ProductCardUiModel;
import com.auction.client.service.AuctionQueryService;
import java.util.List;

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
}