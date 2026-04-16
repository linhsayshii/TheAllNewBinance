package com.auction.client.mapper;

import com.auction.client.dto.ProductCardUiModel;

public class ProductCardMapper {

    public ProductCardUiModel placeholder(int index) {
        return new ProductCardUiModel(
                "Product " + index,
                "Seller " + index,
                "$" + (100 + index * 10),
                "2d 4h"
        );
    }
}