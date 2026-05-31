package com.auction.client.page.create.strategy;

import com.auction.core.dto.auction.ItemAttributesPayload;
import com.auction.core.dto.auction.PrecisionMechanicalPayload;
import com.auction.core.products.CategoryType;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Strategy for Precision Mechanical items: SPORTS, CAMERAS.
 *
 * <p>Renders Model and Warranty Months fields.
 */
public class PrecisionMechanicalStrategy implements CategoryDisplayStrategy {

    private static final String MODEL_ID = "prec_model";
    private static final String WARRANTY_ID = "prec_warrantyMonths";

    @Override
    public List<CategoryType> getSupportedCategoryTypes() {
        return List.of(CategoryType.SPORTS, CategoryType.CAMERAS);
    }

    @Override
    public void renderFields(VBox container) {
        Label modelLabel = new Label("Model");
        modelLabel.getStyleClass().add("create-listing-field-label");
        TextField modelField = new TextField();
        modelField.setId(MODEL_ID);
        modelField.setPromptText("e.g. Canon EOS R5, Garmin Forerunner 955");
        modelField.getStyleClass().add("create-listing-input");

        Label warrantyLabel = new Label("Warranty (months, optional)");
        warrantyLabel.getStyleClass().add("create-listing-field-label");
        TextField warrantyField = new TextField();
        warrantyField.setId(WARRANTY_ID);
        warrantyField.setPromptText("e.g. 12");
        warrantyField.getStyleClass().add("create-listing-input");

        container.getChildren().addAll(modelLabel, modelField, warrantyLabel, warrantyField);
    }

    @Override
    public boolean validateFields(VBox container) {
        TextField model = (TextField) container.lookup("#" + MODEL_ID);
        if (model == null || model.getText() == null || model.getText().isBlank()) {
            return false;
        }
        TextField warranty = (TextField) container.lookup("#" + WARRANTY_ID);
        if (warranty != null && !warranty.getText().isBlank()) {
            try {
                int months = Integer.parseInt(warranty.getText());
                if (months < 0) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemAttributesPayload extractFields(VBox container) {
        TextField model = (TextField) container.lookup("#" + MODEL_ID);
        TextField warranty = (TextField) container.lookup("#" + WARRANTY_ID);

        PrecisionMechanicalPayload payload = new PrecisionMechanicalPayload();
        payload.setModel(model != null ? model.getText() : null);
        if (warranty != null && !warranty.getText().isBlank()) {
            try {
                payload.setWarrantyMonths(Integer.parseInt(warranty.getText()));
            } catch (NumberFormatException ignored) {
                // Leave null if invalid
            }
        }
        return payload;
    }
}
