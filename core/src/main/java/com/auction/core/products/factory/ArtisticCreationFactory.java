package com.auction.core.products.factory;

import com.auction.core.products.ArtisticCreation;
import com.auction.core.products.CategoryType;
import java.util.Map;

/**
 * Factory for ArtisticCreation items (ART, MUSIC). Extracts artist and yearCreated from the attrs
 * map as fixed constructor fields.
 */
public class ArtisticCreationFactory implements ItemFactory {

    @Override
    public CategoryType getSupportedCategory() {
        return CategoryType.ART;
    }

    public CategoryType[] getSupportedCategories() {
        return new CategoryType[] {CategoryType.ART, CategoryType.MUSIC};
    }

    @Override
    public ArtisticCreation createItem(
            Integer id,
            Integer sellerId,
            String name,
            String description,
            String imageUrl,
            Boolean isDeleted,
            Map<String, Object> attrs) {

        String artist = getStr(attrs, "artist");
        Integer yearCreated = getInt(attrs, "yearCreated");

        String catStr = getStr(attrs, "category");
        CategoryType category = CategoryType.ART;
        if (catStr != null) {
            try {
                category = CategoryType.valueOf(catStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fallback to ART
            }
        }

        return new ArtisticCreation(
                id,
                sellerId,
                name,
                description,
                category,
                imageUrl,
                isDeleted,
                artist,
                yearCreated);
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
