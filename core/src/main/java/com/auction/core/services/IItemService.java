package com.auction.core.services;

import com.auction.core.products.Item;

public interface IItemService {
    Item addProduct(int sellerId, String name, String description, String category, String imageUrl);
    void updateProduct(Item item);
    void removeProduct(Item item);
}
