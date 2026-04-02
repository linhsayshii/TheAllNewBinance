package com.auction.client.page.general;

import com.auction.client.dto.ProductCardUiModel;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class GeneralPageController {

    @FXML
    private VBox cardsContainer;

    private final GeneralPageViewModel viewModel = new GeneralPageViewModel();

    @FXML
    private void initialize() {
        cardsContainer.getChildren().clear();
        for (ProductCardUiModel card : viewModel.loadFeaturedAuctions()) {
            Label label = new Label(card.title() + " - " + card.currentBid() + " - " + card.timeLeft());
            label.getStyleClass().add("auction-card-inline");
            cardsContainer.getChildren().add(label);
        }
    }
}