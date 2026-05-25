package com.auction.client.page.create.strategy;

import com.auction.core.dto.auction.ItemAttributesPayload;
import com.auction.core.products.CategoryType;
import java.util.List;
import javafx.scene.layout.VBox;

/**
 * Strategy interface for category-specific dynamic UI rendering.
 *
 * <p>Each concrete implementation handles one <em>group</em> of categories (e.g. Luxury:
 * WATCHES, FASHION, COLLECTIBLES, WINE) and is responsible for:
 *
 * <ol>
 *   <li>Declaring all {@link CategoryType} values it handles via {@link #getSupportedCategoryTypes()}.
 *   <li>Dynamically rendering input fields into a provided {@code VBox} container.
 *   <li>Validating user input within those fields.
 *   <li>Extracting a strongly-typed {@link ItemAttributesPayload} from the filled fields.
 * </ol>
 *
 * <p>Implementations are discovered at runtime via Java SPI ({@link java.util.ServiceLoader}). To
 * register a new strategy, add its fully-qualified class name to:
 * {@code META-INF/services/com.auction.client.page.create.strategy.CategoryDisplayStrategy}
 *
 * <p><b>OCP Compliance</b>: Adding a new product group requires only creating a new
 * implementation class and adding its entry to the SPI config file — no changes to
 * {@code CreateListingController} are needed.
 */
public interface CategoryDisplayStrategy {

    /**
     * Returns all {@link CategoryType} values this strategy can handle. The controller maps each
     * returned type to this strategy instance in its registry, supporting 1-to-many category
     * groupings without registry collisions.
     *
     * @return Non-empty list of supported category types.
     */
    List<CategoryType> getSupportedCategoryTypes();

    /**
     * Populates the given container with the dynamic input fields specific to this product group.
     * The container is cleared by the controller before calling this method.
     *
     * @param container The VBox into which dynamic fields should be added.
     */
    void renderFields(VBox container);

    /**
     * Validates all dynamic fields currently rendered in the container.
     *
     * @param container The VBox containing the rendered dynamic fields.
     * @return {@code true} if all required fields are filled and valid.
     */
    boolean validateFields(VBox container);

    /**
     * Extracts field values from the container and constructs a strongly-typed
     * {@link ItemAttributesPayload} subclass.
     *
     * @param container The VBox containing the rendered dynamic fields.
     * @return A fully populated payload ready to be set on {@code CreateAuctionRequest}.
     */
    ItemAttributesPayload extractFields(VBox container);
}
