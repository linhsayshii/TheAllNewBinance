package com.auction.client.page.general;

import java.io.IOException;
import java.util.List;

import com.auction.client.component.item.AuctionCardComponentController;
import com.auction.client.dto.ProductCardUiModel;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class GeneralPageController {

    private static final String AUCTION_CARD_FXML = "/fxml/components/item/auction-card.fxml";

    @FXML
    private HBox liveAuctionCards;

    private final GeneralPageViewModel viewModel = new GeneralPageViewModel();

    @FXML
    private void initialize() {
        liveAuctionCards.getChildren().clear();

        List<ProductCardUiModel> cards = viewModel.loadFeaturedAuctions();
        int renderCount = 3;

        for (int i = 0; i < renderCount; i++) {
            ProductCardUiModel card = i < cards.size() ? cards.get(i) : null;
            liveAuctionCards.getChildren().add(loadAuctionCard(card, i));
        }
    }

    private VBox loadAuctionCard(ProductCardUiModel card, int index) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(AUCTION_CARD_FXML));

        try {
            VBox cardRoot = loader.load();
            AuctionCardComponentController controller = loader.getController();

            if (card != null) {
                controller.setData(card);
            } else {
                controller.useFakeData(index);
            }

            return cardRoot;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load auction-card component", e);
        }
    }
}