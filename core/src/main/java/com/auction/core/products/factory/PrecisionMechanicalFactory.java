package com.auction.core.products.factory;

import com.auction.core.dto.auction.ItemAttributesPayload;
import com.auction.core.dto.auction.PrecisionMechanicalPayload;
import com.auction.core.products.CategoryType;
import com.auction.core.products.Item;
import com.auction.core.products.PrecisionMechanical;

/**
 * Factory for PrecisionMechanical items (SPORTS, CAMERAS).
 *
 * <p>Accepts a strongly-typed {@link PrecisionMechanicalPayload} and maps its fields directly onto
 * the {@link PrecisionMechanical} constructor, eliminating all Map key string access and manual
 * type casting (Polymorphic Flattening Regression prevention).
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
    public Item createItem(
            Integer id,
            Integer sellerId,
            String name,
            String description,
            String imageUrl,
            Boolean isDeleted,
            ItemAttributesPayload payload) {

        if (!(payload instanceof PrecisionMechanicalPayload precisionPayload)) {
            throw new IllegalArgumentException(
                    "PrecisionMechanicalFactory requires PrecisionMechanicalPayload, got: "
                            + (payload == null ? "null" : payload.getClass().getSimpleName()));
        }

        return new PrecisionMechanical(
                id,
                sellerId,
                name,
                description,
                CategoryType.SPORTS, // Category resolved at DAO insertion from itemCategory string
                imageUrl,
                isDeleted,
                precisionPayload.getModel(),
                precisionPayload.getWarrantyMonths());
    }
}
