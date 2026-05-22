package com.auction.core.products;

import com.auction.core.Entity;

/**
 * Abstract sealed base class for all auctionable products. Sealing this class gives the compiler
 * full visibility of the type hierarchy, enabling exhaustiveness checking in Java 21 switch
 * expressions across the codebase.
 *
 * <p>Three concrete subclasses cover all 8 product categories:
 *
 * <ul>
 *   <li>{@link LuxuryCollectible} – WATCHES, FASHION, COLLECTIBLES, WINE
 *   <li>{@link ArtisticCreation} – ART, MUSIC
 *   <li>{@link PrecisionMechanical} – SPORTS, CAMERAS
 * </ul>
 */
public abstract sealed class Item extends Entity
        permits LuxuryCollectible, ArtisticCreation, PrecisionMechanical {

    private Integer id;
    private Integer sellerId;
    private String name;
    private String description;
    private CategoryType category;
    private String imageUrl;

    protected Item(
            Integer id,
            Integer sellerId,
            String name,
            String description,
            CategoryType category,
            String imageUrl,
            Boolean isDeleted) {
        super();
        this.id = id;
        this.sellerId = sellerId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.imageUrl = imageUrl;
        this.isDeleted = isDeleted != null ? isDeleted : false;
    }

    /**
     * Returns a human-readable summary of the subclass-specific fields. Used for display and
     * logging purposes.
     */
    public abstract String getDetailedSpecs();

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSellerId() {
        return sellerId;
    }

    public void setSellerId(Integer sellerId) {
        this.sellerId = sellerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        updateTimestamp();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        updateTimestamp();
    }

    public CategoryType getCategory() {
        return category;
    }

    public void setCategory(CategoryType category) {
        this.category = category;
        updateTimestamp();
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        updateTimestamp();
    }
}
