package com.auction.client.page.general;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kordamp.ikonli.javafx.FontIcon;

import com.auction.client.component.item.AuctionCardComponentController;
import com.auction.client.component.item.UpcomingAuctionCardComponentController;
import com.auction.client.config.SceneRegistry;
import com.auction.client.dto.ProductCardUiModel;
import com.auction.client.scene.NavigationService;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class GeneralPageController {

    private static final String AUCTION_CARD_FXML = "/fxml/components/item/auction-card.fxml";
    private static final String UPCOMING_AUCTION_CARD_FXML = "/fxml/components/item/upcoming-auction-card.fxml";
    private static final double TARGET_CARD_WIDTH = 240.0;

    @FXML
    private HBox liveAuctionCards;

    @FXML
    private ScrollPane liveCardsScrollPane;

    @FXML
    private HBox liveBlockIndicators;

    @FXML
    private HBox upcomingAuctionCards;

    @FXML
    private ScrollPane upcomingCardsScrollPane;

    @FXML
    private HBox upcomingBlockIndicators;

    @FXML
    private FontIcon themeModeIcon;

    private final GeneralPageViewModel viewModel = new GeneralPageViewModel();
    private final CarouselState liveCarousel = new CarouselState();
    private final CarouselState upcomingCarousel = new CarouselState();

    @FXML
    private void handleGoToLogin() {
        NavigationService.getInstance().openPopup(SceneRegistry.LOGIN_PAGE);
    }

    @FXML
    private void handleGoToRegister() {
        NavigationService.getInstance().openPopup(SceneRegistry.REGISTER_PAGE);
    }

    @FXML
    private void handleToggleTheme() {
        NavigationService.getInstance().toggleTheme();
        refreshThemeModeIcon();
    }

    @FXML
    private void initialize() {
        liveAuctionCards.getChildren().clear();
        upcomingAuctionCards.getChildren().clear();
        refreshThemeModeIcon();

        List<ProductCardUiModel> cards = viewModel.loadFeaturedAuctions();
        int renderCount = 10;

        for (int i = 0; i < renderCount; i++) {
            ProductCardUiModel card = i < cards.size() ? cards.get(i) : null;
            liveAuctionCards.getChildren().add(loadAuctionCard(card, i));
            upcomingAuctionCards.getChildren().add(loadUpcomingAuctionCard(card, i));
        }

        setupCarousel(liveCarousel, liveCardsScrollPane, liveAuctionCards, liveBlockIndicators);
        setupCarousel(upcomingCarousel, upcomingCardsScrollPane, upcomingAuctionCards, upcomingBlockIndicators);
    }

    @FXML
    private void handlePrevLiveBlock() {
        moveToPreviousBlock(liveCarousel);
    }

    @FXML
    private void handleNextLiveBlock() {
        moveToNextBlock(liveCarousel);
    }

    @FXML
    private void handlePrevUpcomingBlock() {
        moveToPreviousBlock(upcomingCarousel);
    }

    @FXML
    private void handleNextUpcomingBlock() {
        moveToNextBlock(upcomingCarousel);
    }

    private void setupCarousel(CarouselState state, ScrollPane scrollPane, HBox cardsContainer, HBox indicatorsContainer) {
        state.scrollPane = scrollPane;
        state.cardsContainer = cardsContainer;
        state.indicatorsContainer = indicatorsContainer;
        state.snapDelay = new PauseTransition(Duration.millis(120));
        state.snapDelay.setOnFinished(event -> snapToNearestBlock(state));

        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            updateCarouselLayout(state);
            snapToCurrentBlock(state);
        });

        scrollPane.hvalueProperty().addListener((obs, oldValue, newValue) -> {
            if (state.programmaticScroll) {
                return;
            }
            state.snapDelay.playFromStart();
        });

        Platform.runLater(() -> {
            updateCarouselLayout(state);
            snapToCurrentBlock(state);
        });
    }

    private void moveToPreviousBlock(CarouselState state) {
        if (state.blockStarts.isEmpty()) {
            return;
        }
        state.currentBlockIndex = Math.max(0, state.currentBlockIndex - 1);
        snapToCurrentBlock(state);
    }

    private void moveToNextBlock(CarouselState state) {
        if (state.blockStarts.isEmpty()) {
            return;
        }
        state.currentBlockIndex = Math.min(state.blockStarts.size() - 1, state.currentBlockIndex + 1);
        snapToCurrentBlock(state);
    }

    private void updateCarouselLayout(CarouselState state) {
        double viewportWidth = state.scrollPane.getViewportBounds().getWidth();
        if (viewportWidth <= 0) {
            return;
        }

        double spacing = state.cardsContainer.getSpacing();
        int totalItems = state.cardsContainer.getChildren().size();
        if (totalItems <= 0) {
            return;
        }

        int itemsPerBlock = Math.max(1, (int) Math.floor((viewportWidth + spacing) / (TARGET_CARD_WIDTH + spacing)));
        state.itemsPerBlock = Math.min(itemsPerBlock, totalItems);

        double cardWidth = (viewportWidth - (state.itemsPerBlock - 1) * spacing) / state.itemsPerBlock;
        for (Node node : state.cardsContainer.getChildren()) {
            if (node instanceof Region region) {
                region.setMinWidth(cardWidth);
                region.setPrefWidth(cardWidth);
                region.setMaxWidth(cardWidth);
            }
        }

        rebuildBlockStarts(state, totalItems);
        rebuildBlockIndicators(state);
        state.currentBlockIndex = Math.min(state.currentBlockIndex, Math.max(0, state.blockStarts.size() - 1));
        updateIndicatorState(state);
    }

    private void rebuildBlockStarts(CarouselState state, int totalItems) {
        state.blockStarts.clear();
        if (totalItems <= 0) {
            return;
        }

        int maxStart = Math.max(0, totalItems - state.itemsPerBlock);
        for (int start = 0; start <= maxStart; start += state.itemsPerBlock) {
            state.blockStarts.add(start);
        }

        if (state.blockStarts.isEmpty() || state.blockStarts.get(state.blockStarts.size() - 1) != maxStart) {
            state.blockStarts.add(maxStart);
        }
    }

    private void rebuildBlockIndicators(CarouselState state) {
        state.indicatorsContainer.getChildren().clear();

        for (int index = 0; index < state.blockStarts.size(); index++) {
            final int blockIndex = index;
            Region indicator = new Region();
            indicator.getStyleClass().add("gp-block-indicator");
            indicator.setOnMouseClicked(event -> {
                state.currentBlockIndex = blockIndex;
                snapToCurrentBlock(state);
            });
            state.indicatorsContainer.getChildren().add(indicator);
        }
    }

    private void snapToNearestBlock(CarouselState state) {
        if (state.blockStarts.isEmpty()) {
            return;
        }

        double currentHValue = state.scrollPane.getHvalue();
        int nearestIndex = 0;
        double nearestDistance = Double.MAX_VALUE;

        for (int i = 0; i < state.blockStarts.size(); i++) {
            double blockPosition = toHValue(state, state.blockStarts.get(i));
            double distance = Math.abs(currentHValue - blockPosition);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = i;
            }
        }

        state.currentBlockIndex = nearestIndex;
        snapToCurrentBlock(state);
    }

    private void snapToCurrentBlock(CarouselState state) {
        if (state.blockStarts.isEmpty()) {
            return;
        }

        int startIndex = state.blockStarts.get(state.currentBlockIndex);
        double hValue = toHValue(state, startIndex);

        state.programmaticScroll = true;
        state.scrollPane.setHvalue(hValue);
        state.programmaticScroll = false;

        updateIndicatorState(state);
    }

    private double toHValue(CarouselState state, int itemStartIndex) {
        int maxStart = Math.max(0, state.cardsContainer.getChildren().size() - state.itemsPerBlock);
        if (maxStart == 0) {
            return 0.0;
        }

        return (double) itemStartIndex / maxStart;
    }

    private void updateIndicatorState(CarouselState state) {
        for (int i = 0; i < state.indicatorsContainer.getChildren().size(); i++) {
            Node indicator = state.indicatorsContainer.getChildren().get(i);
            if (i == state.currentBlockIndex) {
                if (!indicator.getStyleClass().contains("gp-block-indicator-active")) {
                    indicator.getStyleClass().add("gp-block-indicator-active");
                }
            } else {
                indicator.getStyleClass().remove("gp-block-indicator-active");
            }
        }
    }

    private static class CarouselState {
        private ScrollPane scrollPane;
        private HBox cardsContainer;
        private HBox indicatorsContainer;
        private PauseTransition snapDelay;
        private final List<Integer> blockStarts = new ArrayList<>();
        private int itemsPerBlock = 1;
        private int currentBlockIndex = 0;
        private boolean programmaticScroll;
    }

    private void refreshThemeModeIcon() {
        if (themeModeIcon == null) {
            return;
        }

        if (NavigationService.getInstance().isDarkTheme()) {
            themeModeIcon.setIconLiteral("fas-moon");
        } else {
            themeModeIcon.setIconLiteral("fas-sun");
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

    private VBox loadUpcomingAuctionCard(ProductCardUiModel card, int index) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(UPCOMING_AUCTION_CARD_FXML));

        try {
            VBox cardRoot = loader.load();
            UpcomingAuctionCardComponentController controller = loader.getController();

            if (card != null) {
                controller.setData(card);
            } else {
                controller.useFakeData(index);
            }

            return cardRoot;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load upcoming-auction-card component", e);
        }
    }
}