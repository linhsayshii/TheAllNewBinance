package com.auction.client.page.create.strategy;

import com.auction.core.dto.auction.ItemAttributesPayload;
import com.auction.core.dto.auction.LuxuryCollectiblePayload;
import com.auction.core.products.CategoryType;
import java.util.List;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Strategy for Luxury Collectible items: WATCHES, FASHION, COLLECTIBLES, WINE.
 *
 * <p>Renders three shared fields (Brand, Condition, Has Certificate) plus category-specific
 * optional fields: Watch Movement (Watches), Fashion Size (Fashion), Bottle Size (Wine).
 */
public class LuxuryCollectibleStrategy implements CategoryDisplayStrategy {

    private static final String BRAND_ID = "lux_brand";
    private static final String CONDITION_ID = "lux_condition";
    private static final String CERTIFICATE_ID = "lux_certificate";
    private static final String WATCH_MOVEMENT_ID = "lux_watchMovement";
    private static final String FASHION_SIZE_ID = "lux_fashionSize";
    private static final String BOTTLE_SIZE_ID = "lux_bottleSize";

    @Override
    public List<CategoryType> getSupportedCategoryTypes() {
        return List.of(
                CategoryType.WATCHES,
                CategoryType.FASHION,
                CategoryType.COLLECTIBLES,
                CategoryType.WINE);
    }

    @Override
    public void renderFields(VBox container) {
        // Brand
        Label brandLabel = new Label("Brand");
        brandLabel.getStyleClass().add("create-listing-field-label");
        TextField brandField = new TextField();
        brandField.setId(BRAND_ID);
        brandField.setPromptText("e.g. Rolex, Hermès, Château Pétrus");
        brandField.getStyleClass().add("create-listing-input");

        // Condition
        Label conditionLabel = new Label("Condition");
        conditionLabel.getStyleClass().add("create-listing-field-label");
        TextField conditionField = new TextField();
        conditionField.setId(CONDITION_ID);
        conditionField.setPromptText("e.g. Mint, Very Good, Good");
        conditionField.getStyleClass().add("create-listing-input");

        // Has Certificate
        CheckBox certificateBox = new CheckBox("Has Authentication Certificate");
        certificateBox.setId(CERTIFICATE_ID);

        // Optional: Watch Movement
        Label movementLabel = new Label("Watch Movement (optional, for Watches)");
        movementLabel.getStyleClass().add("create-listing-field-label");
        TextField movementField = new TextField();
        movementField.setId(WATCH_MOVEMENT_ID);
        movementField.setPromptText("e.g. Automatic, Quartz, Manual");
        movementField.getStyleClass().add("create-listing-input");

        // Optional: Fashion Size
        Label fashionSizeLabel = new Label("Size (optional, for Fashion)");
        fashionSizeLabel.getStyleClass().add("create-listing-field-label");
        TextField fashionSizeField = new TextField();
        fashionSizeField.setId(FASHION_SIZE_ID);
        fashionSizeField.setPromptText("e.g. S, M, L, EU42");
        fashionSizeField.getStyleClass().add("create-listing-input");

        // Optional: Bottle Size (Wine)
        Label bottleSizeLabel = new Label("Bottle Size in litres (optional, for Wine)");
        bottleSizeLabel.getStyleClass().add("create-listing-field-label");
        TextField bottleSizeField = new TextField();
        bottleSizeField.setId(BOTTLE_SIZE_ID);
        bottleSizeField.setPromptText("e.g. 0.75");
        bottleSizeField.getStyleClass().add("create-listing-input");

        container
                .getChildren()
                .addAll(
                        brandLabel,
                        brandField,
                        conditionLabel,
                        conditionField,
                        certificateBox,
                        movementLabel,
                        movementField,
                        fashionSizeLabel,
                        fashionSizeField,
                        bottleSizeLabel,
                        bottleSizeField);
    }

    @Override
    public boolean validateFields(VBox container) {
        TextField brand = (TextField) container.lookup("#" + BRAND_ID);
        TextField condition = (TextField) container.lookup("#" + CONDITION_ID);
        if (brand == null || brand.getText() == null || brand.getText().isBlank()) {
            return false;
        }
        if (condition == null || condition.getText() == null || condition.getText().isBlank()) {
            return false;
        }
        return true;
    }

    @Override
    public ItemAttributesPayload extractFields(VBox container) {
        TextField brand = (TextField) container.lookup("#" + BRAND_ID);
        TextField condition = (TextField) container.lookup("#" + CONDITION_ID);
        CheckBox certificate = (CheckBox) container.lookup("#" + CERTIFICATE_ID);
        TextField movement = (TextField) container.lookup("#" + WATCH_MOVEMENT_ID);
        TextField fashionSize = (TextField) container.lookup("#" + FASHION_SIZE_ID);
        TextField bottleSize = (TextField) container.lookup("#" + BOTTLE_SIZE_ID);

        LuxuryCollectiblePayload payload = new LuxuryCollectiblePayload();
        payload.setBrand(brand != null ? brand.getText() : null);
        payload.setCondition(condition != null ? condition.getText() : null);
        payload.setHasCertificate(certificate != null && certificate.isSelected());
        payload.setWatchMovement(
                movement != null && !movement.getText().isBlank() ? movement.getText() : null);
        payload.setFashionSize(
                fashionSize != null && !fashionSize.getText().isBlank()
                        ? fashionSize.getText()
                        : null);
        if (bottleSize != null && !bottleSize.getText().isBlank()) {
            try {
                payload.setBottleSize(Double.parseDouble(bottleSize.getText()));
            } catch (NumberFormatException ignored) {
                // Leave null if invalid — server will handle missing optional field
            }
        }
        return payload;
    }
}
