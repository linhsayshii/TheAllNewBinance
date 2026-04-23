package com.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import com.auction.core.products.Item;
import com.auction.server.dao.impl.IItemDao;

public class ItemDao implements IItemDao {
    @Override
    public boolean addItem(Item item) {
        String sql = "INSERT INTO items (seller_id, name, description, category, image_url, is_deleted, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, item.getSellerId());
            stmt.setString(2, item.getName());
            stmt.setString(3, item.getDescription());
            stmt.setString(4, item.getCategory());
            stmt.setString(5, item.getImageUrl() != null ? item.getImageUrl() : "no-image.jpg");
            stmt.setBoolean(6, item.isDeleted());
            // Vì entity Item lưu thời gian tạo là Timestamp, cần truyền Timestamp sql xuống
            stmt.setTimestamp(7, Timestamp.valueOf(item.getCreatedAt()));
            int rowsInserted = stmt.executeUpdate(); // Chạy lệnh INSERT
            if (rowsInserted > 0) {
                // Nếu lưu thành công, MySQL sẽ sinh ra 1 ID. 
                // Ta phải xin lại cái ID đó lắp vào cục Object trên bộ nhớ RAM.
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        item.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error: Cannot save Item!" + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean updateItem(Item item) {
        String sql = "UPDATE items SET seller_id = ?, name = ?, description = ?, category = ?, image_url = ?, updated_at = ? WHERE item_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, item.getSellerId());
            stmt.setString(2, item.getName());
            stmt.setString(3, item.getDescription());
            stmt.setString(4, item.getCategory());
            stmt.setString(5, item.getImageUrl());
            stmt.setTimestamp(6, Timestamp.valueOf(item.getUpdatedAt()));
            stmt.setInt(7, item.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error: Cannot update Item!" + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean deleteItem(Item item) {
        String sql = "UPDATE items SET is_deleted = true, updated_at = ? WHERE item_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(item.getUpdatedAt()));
            stmt.setInt(2, item.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error: Cannot delete Item!" + e.getMessage());
        }
        return false;
    }

    @Override
    public Item findById(int id) {
        String sql = "SELECT * FROM items WHERE item_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Item item = new Item(
                        rs.getInt("item_id"),
                        rs.getInt("seller_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("category"),
                        rs.getString("image_url"),
                        rs.getBoolean("is_deleted")
                    );
                    return item;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error: Cannot find Item!" + e.getMessage());
        }
        return null;
    }
}
