package com.auction.server.services;

import com.auction.core.products.Item;
import com.auction.core.services.IItemService;
import com.auction.server.dao.impl.IItemDao;
import java.util.concurrent.CompletableFuture;

public class ItemService implements IItemService {
    private final IItemDao itemDao;

    public ItemService(IItemDao item) {
        this.itemDao = item;
    }

    @Override
    public CompletableFuture<Item> addProduct(
            int sellerId, String name, String description, String category, String imageUrl) {
        return CompletableFuture.supplyAsync(
                () -> {
                    Item item =
                            new Item(null, sellerId, name, description, category, imageUrl, false);
                    itemDao.addItem(item);
                    return item;
                });
    }

    @Override
    public CompletableFuture<Void> updateProduct(Item item) {
        return CompletableFuture.runAsync(() -> itemDao.updateItem(item));
    }

    @Override
    public CompletableFuture<Void> removeProduct(Item item) {
        return CompletableFuture.runAsync(
                () -> {
                    item.setDeleted(true);
                    itemDao.deleteItem(item);
                });
    }
}
