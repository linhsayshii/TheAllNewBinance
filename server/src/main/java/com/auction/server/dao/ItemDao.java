package com.auction.server.dao;

import com.auction.core.products.ArtisticCreation;
import com.auction.core.products.CategoryType;
import com.auction.core.products.Item;
import com.auction.core.products.LuxuryCollectible;
import com.auction.core.products.PrecisionMechanical;
import com.auction.core.products.attribute.AttributeKey;
import com.auction.core.products.attribute.LuxuryAttributes;
import com.auction.core.products.factory.ItemFactoryProvider;
import com.auction.core.utils.JsonMapper;
import com.auction.server.dao.impl.IItemDao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * JDBC implementation of {@link IItemDao}.
 *
 * <p>Key design decisions:
 *
 * <ul>
 *   <li>All JDBC resources (Connection, PreparedStatement, ResultSet) are managed with
 *       try-with-resources to prevent connection leaks under concurrent WebSocket load.
 *   <li>{@code rs.getObject(..., WrapperClass.class)} is used for nullable primitive DB columns
 *       (has_certificate, year_created, warranty_months) to preserve {@code null} semantics.
 *   <li>Java 21 Pattern Matching switch in {@link #addItem} extracts subclass-specific fields
 *       type-safely without casting or instanceof chains.
 *   <li>Dynamic {@code custom_attributes} (JSON column) are re-inflated into the Heterogeneous
 *       Container of {@link LuxuryCollectible} after factory construction to prevent silent data
 *       loss.
 * </ul>
 */
public class ItemDao implements IItemDao {

    @Override
    public boolean addItem(Item item) {
        String sql =
                "INSERT INTO items (seller_id, name, description, category, image_url,"
                        + " is_deleted, created_at, brand, item_condition, has_certificate,"
                        + " artist, year_created, model, warranty_months, custom_attributes)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt =
                        conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Common fields
            stmt.setInt(1, item.getSellerId());
            stmt.setString(2, item.getName());
            stmt.setString(3, item.getDescription());
            stmt.setString(4, item.getCategory().name());
            stmt.setString(5, item.getImageUrl() != null ? item.getImageUrl() : "no-image.jpg");
            stmt.setBoolean(6, item.isDeleted());
            stmt.setTimestamp(7, Timestamp.valueOf(item.getCreatedAt()));

            // Java 21 Pattern Matching switch: type-safe extraction of subclass-specific fields
            switch (item) {
                case LuxuryCollectible lc -> {
                    stmt.setString(8, lc.getBrand());
                    stmt.setString(9, lc.getCondition());
                    stmt.setObject(10, lc.hasCertificate(), Types.BOOLEAN);
                    stmt.setNull(11, Types.VARCHAR); // artist
                    stmt.setNull(12, Types.INTEGER); // year_created
                    stmt.setNull(13, Types.VARCHAR); // model
                    stmt.setNull(14, Types.INTEGER); // warranty_months

                    // Serialize dynamic attributes into custom_attributes JSON column
                    Map<String, Object> dynAttrs = new HashMap<>();
                    Double size = lc.getAttribute(LuxuryAttributes.BOTTLE_SIZE);
                    if (size != null) {
                        dynAttrs.put(LuxuryAttributes.BOTTLE_SIZE.getName(), size);
                    }
                    String movement = lc.getAttribute(LuxuryAttributes.WATCH_MOVEMENT);
                    if (movement != null) {
                        dynAttrs.put(LuxuryAttributes.WATCH_MOVEMENT.getName(), movement);
                    }
                    String fashionSize = lc.getAttribute(LuxuryAttributes.FASHION_SIZE);
                    if (fashionSize != null) {
                        dynAttrs.put(LuxuryAttributes.FASHION_SIZE.getName(), fashionSize);
                    }
                    stmt.setString(15, dynAttrs.isEmpty() ? null : JsonMapper.toJson(dynAttrs));
                }
                case ArtisticCreation ac -> {
                    stmt.setNull(8, Types.VARCHAR); // brand
                    stmt.setNull(9, Types.VARCHAR); // item_condition
                    stmt.setNull(10, Types.BOOLEAN); // has_certificate
                    stmt.setString(11, ac.getArtist());
                    stmt.setObject(12, ac.getYearCreated(), Types.INTEGER);
                    stmt.setNull(13, Types.VARCHAR); // model
                    stmt.setNull(14, Types.INTEGER); // warranty_months
                    stmt.setNull(15, Types.VARCHAR); // custom_attributes
                }
                case PrecisionMechanical pm -> {
                    stmt.setNull(8, Types.VARCHAR); // brand
                    stmt.setNull(9, Types.VARCHAR); // item_condition
                    stmt.setNull(10, Types.BOOLEAN); // has_certificate
                    stmt.setNull(11, Types.VARCHAR); // artist
                    stmt.setNull(12, Types.INTEGER); // year_created
                    stmt.setString(13, pm.getModel());
                    stmt.setObject(14, pm.getWarrantyMonths(), Types.INTEGER);
                    stmt.setNull(15, Types.VARCHAR); // custom_attributes
                }
            }

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        item.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error: Cannot save Item! " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean updateItem(Item item) {
        String sql =
                "UPDATE items SET seller_id = ?, name = ?, description = ?, category = ?,"
                        + " image_url = ?, updated_at = ? WHERE item_id = ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, item.getSellerId());
            stmt.setString(2, item.getName());
            stmt.setString(3, item.getDescription());
            stmt.setString(4, item.getCategory().name());
            stmt.setString(5, item.getImageUrl());
            stmt.setTimestamp(6, Timestamp.valueOf(item.getUpdatedAt()));
            stmt.setInt(7, item.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error: Cannot update Item! " + e.getMessage());
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
            System.err.println("Error: Cannot delete Item! " + e.getMessage());
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
                    return mapRowToItem(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error: Cannot find Item! " + e.getMessage());
        }
        return null;
    }

    /**
     * Maps a single JDBC ResultSet row to a fully-initialized polymorphic Item instance.
     *
     * <p>Fixed subclass fields are read from their dedicated columns. Dynamic container attributes
     * are parsed from the {@code custom_attributes} JSON column and re-inflated into the {@link
     * LuxuryCollectible} container after factory construction (CRITICAL FIX: prevents silent data
     * loss of dynamic attributes when loading from DB).
     */
    private Item mapRowToItem(ResultSet rs) throws SQLException {
        String categoryStr = rs.getString("category");
        CategoryType category = CategoryType.valueOf(categoryStr.trim().toUpperCase());

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("category", categoryStr);
        attrs.put("brand", rs.getString("brand"));
        attrs.put("condition", rs.getString("item_condition"));
        // Use getObject with wrapper class to preserve null vs false distinction for BOOLEAN NULL
        // columns
        attrs.put("hasCertificate", rs.getObject("has_certificate", Boolean.class));
        attrs.put("artist", rs.getString("artist"));
        attrs.put("yearCreated", rs.getObject("year_created", Integer.class));
        attrs.put("model", rs.getString("model"));
        attrs.put("warrantyMonths", rs.getObject("warranty_months", Integer.class));

        // Parse dynamic attributes JSON (separate tracking for container inflation step)
        Map<String, Object> dynamicAttrs = new HashMap<>();
        String jsonAttrs = rs.getString("custom_attributes");
        if (jsonAttrs != null && !jsonAttrs.trim().isEmpty()) {
            Map<?, ?> parsed = JsonMapper.fromJson(jsonAttrs, Map.class);
            if (parsed != null) {
                for (Map.Entry<?, ?> entry : parsed.entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    dynamicAttrs.put(key, entry.getValue());
                    attrs.put(key, entry.getValue());
                }
            }
        }

        // Create Item via factory
        Item item =
                ItemFactoryProvider.getFactory(category)
                        .createItem(
                                rs.getInt("item_id"),
                                rs.getInt("seller_id"),
                                rs.getString("name"),
                                rs.getString("description"),
                                rs.getString("image_url"),
                                rs.getBoolean("is_deleted"),
                                attrs);

        // CRITICAL FIX: Re-inflate dynamic attributes into Heterogeneous Container
        // The factory only calls the constructor (fixed fields); the container would be empty
        // without this step when rehydrating from the database.
        if (item instanceof LuxuryCollectible lc) {
            for (Map.Entry<String, Object> entry : dynamicAttrs.entrySet()) {
                AttributeKey<?> key = AttributeKey.getByName(entry.getKey());
                if (key != null && entry.getValue() != null) {
                    Object safeVal = castToTargetType(entry.getValue(), key.getType());
                    putAttributeHelper(lc, key, safeVal);
                }
            }
        }

        return item;
    }

    /**
     * Converts a raw {@link Number} to the declared target type. Handles both Gson (Double) and
     * JDBC (Integer, Long, BigDecimal) numeric types.
     */
    private Object castToTargetType(Object rawVal, Class<?> targetType) {
        if (rawVal instanceof Number numberVal) {
            if (targetType == Integer.class || targetType == int.class) {
                return numberVal.intValue();
            } else if (targetType == Long.class || targetType == long.class) {
                return numberVal.longValue();
            } else if (targetType == Float.class || targetType == float.class) {
                return numberVal.floatValue();
            } else if (targetType == Double.class || targetType == double.class) {
                return numberVal.doubleValue();
            }
        }
        return rawVal;
    }

    @SuppressWarnings("unchecked")
    private <T> void putAttributeHelper(LuxuryCollectible lc, AttributeKey<T> key, Object value) {
        lc.putAttribute(key, (T) value);
    }
}
