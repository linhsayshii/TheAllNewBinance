package com.auction.server.dao;

import com.auction.core.products.Item;

public interface IItemDao {
    public boolean addItem(Item item);
    public boolean updateItem(Item item);
    public Item findById(int id);
    public boolean deleteItem(Item item);
}
