package com.auction.core.services;

import com.auction.core.products.Item;

public interface IProductService {
    Item addProduct(Integer sellerId, String name, String description, String category, String imageUrl);
    void removeProduct(Item item);
    void updateProduct(Item item);
}
