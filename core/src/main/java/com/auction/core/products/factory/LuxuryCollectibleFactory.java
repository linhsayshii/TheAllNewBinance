package com.auction.core.products.factory;

import com.auction.core.dto.auction.ItemAttributesPayload;
import com.auction.core.dto.auction.LuxuryCollectiblePayload;
import com.auction.core.products.CategoryType;
import com.auction.core.products.Item;
import com.auction.core.products.LuxuryCollectible;
import com.auction.core.products.attribute.LuxuryAttributes;

/**
 * Factory for LuxuryCollectible items (WATCHES, FASHION, COLLECTIBLES, WINE).
 *
 * <p>Accepts a strongly-typed {@link LuxuryCollectiblePayload} and maps its fields directly onto
 * the {@link LuxuryCollectible} constructor and Heterogeneous Container, eliminating all Map key
 * string access and manual type casting (Polymorphic Flattening Regression prevention).
 */
public class LuxuryCollectibleFactory implements ItemFactory {

    @Override
    public CategoryType getSupportedCategory() {
        return CategoryType.WATCHES;
    }

    /**
     * Returns all categories this factory handles, for multi-registration in ItemFactoryProvider.
     */
    public CategoryType[] getSupportedCategories() {
        return new CategoryType[] {
            CategoryType.WATCHES, CategoryType.FASHION, CategoryType.COLLECTIBLES, CategoryType.WINE
        };
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

        if (!(payload instanceof LuxuryCollectiblePayload luxuryPayload)) {
            throw new IllegalArgumentException(
                    "LuxuryCollectibleFactory requires LuxuryCollectiblePayload, got: "
                            + (payload == null ? "null" : payload.getClass().getSimpleName()));
        }

        // Category will be set to WATCHES as default; actual category is persisted via the
        // itemCategory string in the DB column — the domain object carries it for filtering.
        LuxuryCollectible item =
                new LuxuryCollectible(
                        id,
                        sellerId,
                        name,
                        description,
                        CategoryType.WATCHES,
                        imageUrl,
                        isDeleted,
                        luxuryPayload.getBrand(),
                        luxuryPayload.getCondition(),
                        luxuryPayload.isHasCertificate());

        // Push optional dynamic attributes into the Heterogeneous Container
        if (luxuryPayload.getWatchMovement() != null) {
            item.putAttribute(LuxuryAttributes.WATCH_MOVEMENT, luxuryPayload.getWatchMovement());
        }
        if (luxuryPayload.getBottleSize() != null) {
            item.putAttribute(LuxuryAttributes.BOTTLE_SIZE, luxuryPayload.getBottleSize());
        }
        if (luxuryPayload.getFashionSize() != null) {
            item.putAttribute(LuxuryAttributes.FASHION_SIZE, luxuryPayload.getFashionSize());
        }
        return item;
    }
}
