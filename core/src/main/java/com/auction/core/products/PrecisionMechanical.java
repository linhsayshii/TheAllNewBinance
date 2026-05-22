package com.auction.core.products;

/**
 * Concrete sealed subclass for sports equipment and camera/electronics items. Covers categories:
 * SPORTS, CAMERAS.
 *
 * <p>Fixed fields: model identifier and warranty duration in months.
 */
public final class PrecisionMechanical extends Item {

    private final String model;
    private final Integer warrantyMonths;

    public PrecisionMechanical(
            Integer id,
            Integer sellerId,
            String name,
            String description,
            CategoryType category,
            String imageUrl,
            Boolean isDeleted,
            String model,
            Integer warrantyMonths) {
        super(id, sellerId, name, description, category, imageUrl, isDeleted);
        this.model = model;
        this.warrantyMonths = warrantyMonths;
    }

    public String getModel() {
        return model;
    }

    public Integer getWarrantyMonths() {
        return warrantyMonths;
    }

    @Override
    public String getDetailedSpecs() {
        String warranty = warrantyMonths != null ? warrantyMonths + " months" : "No warranty";
        return String.format("Model: %s | Warranty: %s", model, warranty);
    }
}
