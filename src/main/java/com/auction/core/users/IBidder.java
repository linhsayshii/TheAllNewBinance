package com.auction.core.users;

import com.auction.core.products.KhoToiUu;

public interface IBidder {
    void placeBid(String productId, double amount, KhoToiUu kho);
}