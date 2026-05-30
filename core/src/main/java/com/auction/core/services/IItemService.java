package com.auction.core.services;

import com.auction.core.products.Item;
import java.util.concurrent.CompletableFuture;

public interface IItemService {
    CompletableFuture<Item> addProduct(
            int sellerId, String name, String description, String category, String imageUrl);

    CompletableFuture<Void> updateProduct(Item item);

    CompletableFuture<Void> removeProduct(Item item);
}
