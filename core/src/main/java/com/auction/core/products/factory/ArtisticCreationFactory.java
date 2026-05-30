package com.auction.core.products.factory;

import com.auction.core.dto.auction.ArtisticCreationPayload;
import com.auction.core.dto.auction.ItemAttributesPayload;
import com.auction.core.products.ArtisticCreation;
import com.auction.core.products.CategoryType;
import com.auction.core.products.Item;

/**
 * Factory for ArtisticCreation items (ART, MUSIC).
 *
 * <p>Accepts a strongly-typed {@link ArtisticCreationPayload} and maps its fields directly onto the
 * {@link ArtisticCreation} constructor, eliminating all Map key string access and manual type
 * casting (Polymorphic Flattening Regression prevention).
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
    public Item createItem(
            Integer id,
            Integer sellerId,
            String name,
            String description,
            String imageUrl,
            Boolean isDeleted,
            ItemAttributesPayload payload) {

        if (!(payload instanceof ArtisticCreationPayload artisticPayload)) {
            throw new IllegalArgumentException(
                    "ArtisticCreationFactory requires ArtisticCreationPayload, got: "
                            + (payload == null ? "null" : payload.getClass().getSimpleName()));
        }

        // Category will always be ART or MUSIC; default to ART — the caller sets itemCategory
        // string separately for DB persistence, but the factory domain object uses ART as base.
        return new ArtisticCreation(
                id,
                sellerId,
                name,
                description,
                CategoryType.ART, // Category resolved at DAO insertion from itemCategory string
                imageUrl,
                isDeleted,
                artisticPayload.getArtist(),
                artisticPayload.getYearCreated());
    }
}
