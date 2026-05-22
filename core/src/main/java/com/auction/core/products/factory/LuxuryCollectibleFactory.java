package com.auction.core.products.factory;

import com.auction.core.products.CategoryType;
import com.auction.core.products.LuxuryCollectible;
import com.auction.core.products.attribute.AttributeKey;
import java.util.Map;

/**
 * Factory for LuxuryCollectible items (WATCHES, FASHION, COLLECTIBLES, WINE).
 *
 * <p>Fixed fields (brand, condition, hasCertificate) are extracted from the attrs map and passed to
 * the constructor. Dynamic AttributeKeys recognized in the KEY_POOL (e.g. BOTTLE_SIZE,
 * WATCH_MOVEMENT, FASHION_SIZE) are then pushed into the Heterogeneous Container.
 */
public class LuxuryCollectibleFactory implements ItemFactory {

    @Override
    public CategoryType getSupportedCategory() {
        // This factory handles all 4 luxury categories.
        // ItemFactoryProvider registers this factory once per category
        // by iterating supportedCategories() if needed; for SPI single-instance,
        // we return the primary category here. The provider will handle multi-category
        // registration in its loop.
        return CategoryType.WATCHES;
    }

    /** Returns all categories this factory handles, for multi-registration. */
    public CategoryType[] getSupportedCategories() {
        return new CategoryType[] {
            CategoryType.WATCHES, CategoryType.FASHION, CategoryType.COLLECTIBLES, CategoryType.WINE
        };
    }

    @Override
    public LuxuryCollectible createItem(
            Integer id,
            Integer sellerId,
            String name,
            String description,
            String imageUrl,
            Boolean isDeleted,
            Map<String, Object> attrs) {

        // Extract fixed constructor fields from attrs map
        String brand = getStr(attrs, "brand");
        String condition = getStr(attrs, "condition");
        Boolean certObj = (Boolean) attrs.get("hasCertificate");
        boolean hasCertificate = certObj != null && certObj;

        // Category is passed via attrs so factories can read it when needed
        String catStr = getStr(attrs, "category");
        CategoryType category = CategoryType.WATCHES;
        if (catStr != null) {
            try {
                category = CategoryType.valueOf(catStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fallback to WATCHES
            }
        }

        LuxuryCollectible item =
                new LuxuryCollectible(
                        id,
                        sellerId,
                        name,
                        description,
                        category,
                        imageUrl,
                        isDeleted,
                        brand,
                        condition,
                        hasCertificate);

        // Push dynamic attributes recognized in KEY_POOL into the Heterogeneous Container
        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            AttributeKey<?> key = AttributeKey.getByName(entry.getKey());
            if (key != null && entry.getValue() != null) {
                putAttributeHelper(item, key, entry.getValue());
            }
        }

        return item;
    }

    @SuppressWarnings("unchecked")
    private <T> void putAttributeHelper(LuxuryCollectible lc, AttributeKey<T> key, Object value) {
        lc.putAttribute(key, (T) value);
    }

    private String getStr(Map<String, Object> attrs, String key) {
        Object val = attrs.get(key);
        return val instanceof String ? (String) val : null;
    }
}
