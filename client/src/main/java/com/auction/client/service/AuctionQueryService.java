package com.auction.client.service;

import com.auction.client.dto.ProductCardUiModel;
import com.auction.client.mapper.ProductCardMapper;
import java.util.List;
import java.util.stream.IntStream;

public class AuctionQueryService {

    private final ProductCardMapper mapper = new ProductCardMapper();

    public List<ProductCardUiModel> getFeaturedAuctions() {
        return IntStream.rangeClosed(1, 6)
                .mapToObj(mapper::placeholder)
                .toList();
    }
}