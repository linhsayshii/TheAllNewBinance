package com.auction.core.products.factory;

import com.auction.core.dto.auction.ItemAttributesPayload;
import com.auction.core.products.CategoryType;
import com.auction.core.products.Item;

/**
 * Service Provider Interface for polymorphic Item creation. Each concrete factory handles one
 * product group and declares which {@link CategoryType} it supports via {@link
 * #getSupportedCategory()}.
 *
 * <p>The {@code payload} parameter is a strongly-typed {@link ItemAttributesPayload} subclass
 * deserialized at the Socket boundary, ensuring 100% Type-Safety from Network Boundary through to
 * Domain Layer. No manual casting or Map key access is required inside factory implementations.
 *
 * <p>Concrete factories are responsible for downcasting {@code payload} to their expected subtype
 * using Java 21 Pattern Matching ({@code instanceof} with binding variable).
 */
public interface ItemFactory {

    /**
     * Declares which CategoryType this factory handles. Used by {@link ItemFactoryProvider} to
     * build the registry without circular dependency.
     */
    CategoryType getSupportedCategory();

    /**
     * Creates a fully initialized Item subclass instance.
     *
     * @param id Database-generated item ID (may be null for new items).
     * @param sellerId ID of the seller.
     * @param name Display name of the item.
     * @param description Full description.
     * @param imageUrl URL to the item image.
     * @param isDeleted Soft-delete flag.
     * @param payload Strongly-typed payload carrying product-group-specific attributes. The factory
     *     implementation must downcast to the expected concrete subtype.
     * @return A fully constructed and populated Item instance.
     * @throws IllegalArgumentException if the payload subtype does not match the factory's product
     *     group.
     */
    Item createItem(
            Integer id,
            Integer sellerId,
            String name,
            String description,
            String imageUrl,
            Boolean isDeleted,
            ItemAttributesPayload payload);
}
