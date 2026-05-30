package com.auction.server.dao.impl;

import com.auction.core.products.Item;
import java.sql.Connection;
import java.sql.SQLException;

public interface IItemDao {
    boolean addItem(Item item);

    /**
     * Inserts an Item using a caller-provided Connection for Transactional (Atomic) dual-write. The
     * caller is responsible for commit/rollback lifecycle of the connection.
     */
    boolean addItemWithConnection(Connection conn, Item item) throws SQLException;

    boolean updateItem(Item item);

    Item findById(int id);

    boolean deleteItem(Item item);
}
