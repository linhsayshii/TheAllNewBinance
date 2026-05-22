package com.auction.core.products.factory;

import com.auction.core.products.CategoryType;
import com.auction.core.products.Item;
import java.util.Map;

/**
 * Service Provider Interface for polymorphic Item creation. Each concrete factory handles one
 * product group and declares which {@link CategoryType} it supports via {@link
 * #getSupportedCategory()}.
 *
 * <p>The {@code attrs} map passed to {@link #createItem} contains a mix of:
 *
 * <ul>
 *   <li>Fixed subclass fields (e.g. "brand", "condition") – extracted by the factory and passed as
 *       constructor arguments.
 *   <li>Dynamic container attributes (e.g. "bottleSize") – pushed into the Heterogeneous Container
 *       after object construction.
 * </ul>
 *
 * Concrete factories are responsible for clearly separating these two concerns.
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
     * @param attrs Mixed map of fixed fields and dynamic attributes (all pre-normalized to target
     *     types).
     * @return A fully constructed and populated Item instance.
     */
    Item createItem(
            Integer id,
            Integer sellerId,
            String name,
            String description,
            String imageUrl,
            Boolean isDeleted,
            Map<String, Object> attrs);
}
