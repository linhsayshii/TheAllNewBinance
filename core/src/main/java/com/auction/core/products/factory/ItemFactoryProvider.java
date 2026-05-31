package com.auction.core.products.factory;

import com.auction.core.products.CategoryType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 * Registry for polymorphic Item factories, loaded via Java SPI at startup.
 *
 * <p>Lifecycle:
 *
 * <ol>
 *   <li>{@link #initialize()} is called once at Application Startup (before any WebSocket traffic).
 *   <li>SPI discovers all {@link ItemFactory} implementations from {@code META-INF/services}.
 *   <li>Each factory declares its supported categories via {@link
 *       ItemFactory#getSupportedCategory()} and optionally {@code getSupportedCategories()} for
 *       multi-category factories.
 *   <li>The registry map is frozen with {@link Collections#unmodifiableMap} to guarantee zero
 *       mutation during multi-threaded runtime – no locks needed for reads.
 * </ol>
 *
 * <p>DIP compliance: Concrete factories do NOT call back into this provider. The provider is the
 * sole authority over registration.
 */
public class ItemFactoryProvider {

    private static final Logger LOGGER = Logger.getLogger(ItemFactoryProvider.class.getName());

    private static Map<CategoryType, ItemFactory> registry = new HashMap<>();
    private static volatile boolean isFrozen = false;

    private ItemFactoryProvider() {
        // Utility class – no instantiation
    }

    /**
     * Scans SPI providers, registers each factory for all its supported categories, then freezes
     * the registry permanently. Idempotent – subsequent calls are ignored.
     */
    public static synchronized void initialize() {
        if (isFrozen) {
            return;
        }

        LOGGER.info("Scanning Item Factories via Java SPI...");
        ServiceLoader<ItemFactory> loader = ServiceLoader.load(ItemFactory.class);

        for (ItemFactory factory : loader) {
            // Support multi-category factories via getSupportedCategories() if available
            try {
                CategoryType[] categories =
                        (CategoryType[])
                                factory.getClass()
                                        .getMethod("getSupportedCategories")
                                        .invoke(factory);
                for (CategoryType cat : categories) {
                    registry.put(cat, factory);
                    LOGGER.info(
                            "  Registered " + factory.getClass().getSimpleName() + " -> " + cat);
                }
            } catch (Exception e) {
                // Fallback: use single category declared by interface method
                CategoryType cat = factory.getSupportedCategory();
                registry.put(cat, factory);
                LOGGER.info("  Registered " + factory.getClass().getSimpleName() + " -> " + cat);
            }
        }

        registry = Collections.unmodifiableMap(registry);
        isFrozen = true;
        LOGGER.info("Item Factory registry frozen. " + registry.size() + " categories registered.");
    }

    /**
     * Returns the factory for the given category.
     *
     * @throws IllegalArgumentException if no factory is registered for the category.
     * @throws IllegalStateException if {@link #initialize()} has not been called yet.
     */
    public static ItemFactory getFactory(CategoryType category) {
        if (!isFrozen) {
            throw new IllegalStateException(
                    "ItemFactoryProvider not initialized. Call initialize() at startup.");
        }
        ItemFactory factory = registry.get(category);
        if (factory == null) {
            throw new IllegalArgumentException(
                    "No ItemFactory registered for category: " + category);
        }
        return factory;
    }
}
