package com.auction.core.users;

import com.auction.core.products.KhoToiUu;

public interface ISeller {
    void addProduct(String productName, double startingPrice, KhoToiUu kho);
    void removeProduct(String productId, KhoToiUu kho);
}