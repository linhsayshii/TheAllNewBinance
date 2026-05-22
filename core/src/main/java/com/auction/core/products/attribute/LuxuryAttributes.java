package com.auction.core.products.attribute;

/**
 * Catalog of well-known dynamic AttributeKeys for LuxuryCollectible items. Declaring keys as
 * interface constants ensures they are initialized (and registered into KEY_POOL) the moment this
 * interface is class-loaded by the JVM.
 *
 * <p>Add new keys here whenever a new dynamic luxury attribute is required. The string names used
 * here are the canonical keys used in flat JSON payloads and DB columns.
 */
public interface LuxuryAttributes {

    /** Volume size in litres, applicable to WINE category. */
    AttributeKey<Double> BOTTLE_SIZE = AttributeKey.of("bottleSize", Double.class);

    /** Movement type (e.g. "Automatic", "Quartz"), applicable to WATCHES category. */
    AttributeKey<String> WATCH_MOVEMENT = AttributeKey.of("movement", String.class);

    /** Clothing size (e.g. "M", "XL", "EU42"), applicable to FASHION category. */
    AttributeKey<String> FASHION_SIZE = AttributeKey.of("fashionSize", String.class);
}
