package com.auction.core.services;

import com.auction.core.products.Item;

public interface IItemService {
    Item addProduct(Integer sellerId, String name, String description, String category, String imageUrl);
    void updateProduct(Item item);
    void removeProduct(Item item);
}
