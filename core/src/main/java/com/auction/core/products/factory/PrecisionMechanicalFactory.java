package com.auction.core.products.factory;

import com.auction.core.products.CategoryType;
import com.auction.core.products.PrecisionMechanical;
import java.util.Map;

/**
 * Factory for PrecisionMechanical items (SPORTS, CAMERAS). Extracts model and warrantyMonths from
 * the attrs map as fixed constructor fields.
 */
public class PrecisionMechanicalFactory implements ItemFactory {

    @Override
    public CategoryType getSupportedCategory() {
        return CategoryType.SPORTS;
    }

    public CategoryType[] getSupportedCategories() {
        return new CategoryType[] {CategoryType.SPORTS, CategoryType.CAMERAS};
    }

    @Override
    public PrecisionMechanical createItem(
            Integer id,
            Integer sellerId,
            String name,
            String description,
            String imageUrl,
            Boolean isDeleted,
            Map<String, Object> attrs) {

        String model = getStr(attrs, "model");
        Integer warrantyMonths = getInt(attrs, "warrantyMonths");

        String catStr = getStr(attrs, "category");
        CategoryType category = CategoryType.SPORTS;
        if (catStr != null) {
            try {
                category = CategoryType.valueOf(catStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fallback to SPORTS
            }
        }

        return new PrecisionMechanical(
                id,
                sellerId,
                name,
                description,
                category,
                imageUrl,
                isDeleted,
                model,
                warrantyMonths);
    }

    private String getStr(Map<String, Object> attrs, String key) {
        Object val = attrs.get(key);
        return val instanceof String ? (String) val : null;
    }

    private Integer getInt(Map<String, Object> attrs, String key) {
        Object val = attrs.get(key);
        if (val instanceof Integer i) {
            return i;
        }
        if (val instanceof Number n) {
            return n.intValue();
        }
        return null;
    }
}
