package com.auction.client.page.create.strategy;

import com.auction.core.dto.auction.ArtisticCreationPayload;
import com.auction.core.dto.auction.ItemAttributesPayload;
import com.auction.core.products.CategoryType;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Strategy for Artistic Creation items: ART, MUSIC.
 *
 * <p>Renders Artist and Year Created fields.
 */
public class ArtisticCreationStrategy implements CategoryDisplayStrategy {

    private static final String ARTIST_ID = "art_artist";
    private static final String YEAR_ID = "art_yearCreated";

    @Override
    public List<CategoryType> getSupportedCategoryTypes() {
        return List.of(CategoryType.ART, CategoryType.MUSIC);
    }

    @Override
    public void renderFields(VBox container) {
        Label artistLabel = new Label("Artist / Creator");
        artistLabel.getStyleClass().add("create-listing-field-label");
        TextField artistField = new TextField();
        artistField.setId(ARTIST_ID);
        artistField.setPromptText("e.g. Vincent van Gogh, The Beatles");
        artistField.getStyleClass().add("create-listing-input");

        Label yearLabel = new Label("Year Created (optional)");
        yearLabel.getStyleClass().add("create-listing-field-label");
        TextField yearField = new TextField();
        yearField.setId(YEAR_ID);
        yearField.setPromptText("e.g. 1889");
        yearField.getStyleClass().add("create-listing-input");

        container.getChildren().addAll(artistLabel, artistField, yearLabel, yearField);
    }

    @Override
    public boolean validateFields(VBox container) {
        TextField artist = (TextField) container.lookup("#" + ARTIST_ID);
        if (artist == null || artist.getText() == null || artist.getText().isBlank()) {
            return false;
        }
        TextField year = (TextField) container.lookup("#" + YEAR_ID);
        if (year != null && !year.getText().isBlank()) {
            try {
                int y = Integer.parseInt(year.getText());
                if (y < 1 || y > 9999) {
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
        TextField artist = (TextField) container.lookup("#" + ARTIST_ID);
        TextField year = (TextField) container.lookup("#" + YEAR_ID);

        ArtisticCreationPayload payload = new ArtisticCreationPayload();
        payload.setArtist(artist != null ? artist.getText() : null);
        if (year != null && !year.getText().isBlank()) {
            try {
                payload.setYearCreated(Integer.parseInt(year.getText()));
            } catch (NumberFormatException ignored) {
                // Leave null if year is invalid
            }
        }
        return payload;
    }
}
