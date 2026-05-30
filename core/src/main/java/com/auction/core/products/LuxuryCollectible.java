package com.auction.core.products;

import com.auction.core.products.attribute.AttributeKey;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete sealed subclass for luxury and collectible items. Covers categories: WATCHES, FASHION,
 * COLLECTIBLES, WINE.
 *
 * <p>In addition to the fixed fields (brand, condition, hasCertificate), this class contains a
 * Typesafe Heterogeneous Container ({@code attributes}) for category-specific dynamic properties
 * (e.g. bottle size for WINE, movement type for WATCHES) without requiring additional subclasses.
 */
public final class LuxuryCollectible extends Item {

    private final String brand;
    private final String condition;
    private final boolean hasCertificate;

    /** Heterogeneous Container: type-safe dynamic attributes specific to this luxury item. */
    private final Map<AttributeKey<?>, Object> attributes = new ConcurrentHashMap<>();

    public LuxuryCollectible(
            Integer id,
            Integer sellerId,
            String name,
            String description,
            CategoryType category,
            String imageUrl,
            Boolean isDeleted,
            String brand,
            String condition,
            boolean hasCertificate) {
        super(id, sellerId, name, description, category, imageUrl, isDeleted);
        this.brand = brand;
        this.condition = condition;
        this.hasCertificate = hasCertificate;
    }

    /**
     * Inserts a typed attribute into the Heterogeneous Container. The compiler enforces that the
     * value type matches the key's declared type.
     *
     * @param key A typed AttributeKey.
     * @param value A non-null value of the correct type.
     * @param <T> Value type.
     */
    public <T> void putAttribute(AttributeKey<T> key, T value) {
        attributes.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
    }

    /**
     * Retrieves a typed attribute from the Heterogeneous Container. The cast is guaranteed safe by
     * the container invariant.
     *
     * @param key A typed AttributeKey.
     * @param <T> Value type.
     * @return The value, or null if absent.
     */
    public <T> T getAttribute(AttributeKey<T> key) {
        Object val = attributes.get(Objects.requireNonNull(key));
        return val != null ? key.getType().cast(val) : null;
    }

    public String getBrand() {
        return brand;
    }

    public String getCondition() {
        return condition;
    }

    public boolean hasCertificate() {
        return hasCertificate;
    }

    @Override
    public String getDetailedSpecs() {
        return String.format(
                "Brand: %s | Condition: %s | Certificate: %s",
                brand, condition, hasCertificate ? "Yes" : "No");
    }
}
