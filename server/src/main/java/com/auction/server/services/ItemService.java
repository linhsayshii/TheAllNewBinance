package com.auction.server.services;

import com.auction.core.dao.IItemDao;
import com.auction.core.products.Item;
import com.auction.core.services.IItemService;

public class ItemService implements IItemService {
    private final IItemDao itemDao;
    public ItemService(IItemDao item) {
        this.itemDao = item;
    }
    @Override
    public Item addProduct(Integer sellerId, String name, String description, String category, String imageUrl) {
        Item item = new Item(null, sellerId, name, description, category, imageUrl, false);
        itemDao.addItem(item);
        return item;
    }

    @Override
    public void updateProduct(Item item) {
        itemDao.updateItem(item);
    }

    @Override
    public void removeProduct(Item item) {
        item.setDeleted(true);
        itemDao.deleteItem(item);
    }
}
