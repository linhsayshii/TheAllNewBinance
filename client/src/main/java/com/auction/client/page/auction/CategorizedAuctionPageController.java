package com.auction.client.page.auction;

import com.auction.client.component.item.AuctionCardComponentController;
import com.auction.client.component.item.EndedAuctionCardComponentController;
import com.auction.client.component.item.UpcomingAuctionCardComponentController;
import com.auction.client.dto.ProductCardUiModel;
import com.auction.client.exception.SceneLoadException;
import com.auction.client.scene.LifecycleAwareController;
import com.auction.client.service.NetworkService;
import com.auction.core.protocol.EventType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

public class CategorizedAuctionPageController implements LifecycleAwareController {

    private static final String AUCTION_CARD_FXML = "/fxml/components/item/auction-card.fxml";
    private static final String UPCOMING_CARD_FXML =
            "/fxml/components/item/upcoming-auction-card.fxml";
    private static final String ENDED_CARD_FXML = "/fxml/components/item/ended-auction-card.fxml";
    private static final String HANDLER_ID = "CategorizedAuctionPage";
    private static final double TARGET_CARD_WIDTH = 400.0;

    @FXML private Button btnLuxury;
    @FXML private Button btnArtistic;
    @FXML private Button btnPrecision;
    @FXML private VBox subcategoriesContainer;

    private final CategorizedAuctionPageViewModel viewModel = new CategorizedAuctionPageViewModel();
    private final List<CarouselState> activeCarousels = new ArrayList<>();
    private String selectedItemType = "LuxuryCollectible";

    @FXML
    private void initialize() {
        selectTab(selectedItemType);
        registerNetworkHandlers();
    }

    private void registerNetworkHandlers() {
        try {
            NetworkService.getInstance()
                    .getClient()
                    .addResponseHandler(
                            EventType.AUCTION_ACTIVATED,
                            HANDLER_ID,
                            message -> refreshCurrentTab());
            NetworkService.getInstance()
                    .getClient()
                    .addResponseHandler(
                            EventType.AUCTION_CLOSED,
                            HANDLER_ID,
                            message -> refreshCurrentTab());
            NetworkService.getInstance()
                    .getClient()
                    .addResponseHandler(
                            EventType.PROMOTE_AUCTION,
                            HANDLER_ID,
                            message -> refreshCurrentTab());

            NetworkService.getInstance()
                    .sendRequest(
                            EventType.SUBSCRIBE_AUCTION, java.util.Map.of("auctionId", 0));
        } catch (Exception e) {
            System.err.println("[CategorizedAuctionPage] Failed to register handlers: "
                    + e.getMessage());
        }
    }

    private void refreshCurrentTab() {
        selectTab(selectedItemType);
    }

    @Override
    public void onUnload() {
        try {
            NetworkService.getInstance()
                    .sendRequest(EventType.UNSUBSCRIBE_AUCTION, Map.of("auctionId", 0));
            NetworkService.getInstance()
                    .getClient()
                    .removeResponseHandler(EventType.AUCTION_ACTIVATED, HANDLER_ID);
            NetworkService.getInstance()
                    .getClient()
                    .removeResponseHandler(EventType.AUCTION_CLOSED, HANDLER_ID);
            NetworkService.getInstance()
                    .getClient()
                    .removeResponseHandler(EventType.PROMOTE_AUCTION, HANDLER_ID);
        } catch (Exception ignored) {
        }
    }

    @FXML
    private void handleSelectLuxury() {
        selectTab("LuxuryCollectible");
    }

    @FXML
    private void handleSelectArtistic() {
        selectTab("ArtisticCreation");
    }

    @FXML
    private void handleSelectPrecision() {
        selectTab("PrecisionMechanical");
    }

    private void selectTab(String itemType) {
        this.selectedItemType = itemType;
        updateTabStyles(itemType);
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            List<ProductCardUiModel> cards = viewModel.loadAuctionsByItemType(itemType);
            Platform.runLater(() -> populateSubcategories(itemType, cards));
        });
    }

    private void updateTabStyles(String itemType) {
        btnLuxury.getStyleClass().remove("gp-category-tab-btn-active");
        btnArtistic.getStyleClass().remove("gp-category-tab-btn-active");
        btnPrecision.getStyleClass().remove("gp-category-tab-btn-active");

        if ("LuxuryCollectible".equals(itemType)) {
            btnLuxury.getStyleClass().add("gp-category-tab-btn-active");
        } else if ("ArtisticCreation".equals(itemType)) {
            btnArtistic.getStyleClass().add("gp-category-tab-btn-active");
        } else {
            btnPrecision.getStyleClass().add("gp-category-tab-btn-active");
        }
    }

    private void populateSubcategories(String itemType, List<ProductCardUiModel> cards) {
        subcategoriesContainer.getChildren().clear();
        activeCarousels.clear();

        List<SubcategoryDef> defs = getSubcategoryDefs(itemType);
        Map<String, List<ProductCardUiModel>> grouped = new HashMap<>();
        for (SubcategoryDef def : defs) {
            grouped.put(def.key().toUpperCase(), new ArrayList<>());
        }

        for (ProductCardUiModel card : cards) {
            if (card.category() != null) {
                String key = card.category().trim().toUpperCase();
                if (grouped.containsKey(key)) {
                    grouped.get(key).add(card);
                }
            }
        }

        for (SubcategoryDef def : defs) {
            CarouselState carousel = new CarouselState();
            VBox row = buildSubcategoryRow(
                    def.displayName(), grouped.get(def.key().toUpperCase()), carousel);
            subcategoriesContainer.getChildren().add(row);
            activeCarousels.add(carousel);
        }
    }

    private List<SubcategoryDef> getSubcategoryDefs(String itemType) {
        if ("LuxuryCollectible".equals(itemType)) {
            return List.of(
                    new SubcategoryDef("WATCHES", "Watches"),
                    new SubcategoryDef("FASHION", "Fashion"),
                    new SubcategoryDef("COLLECTIBLES", "Collectibles"),
                    new SubcategoryDef("WINE", "Wine"));
        } else if ("ArtisticCreation".equals(itemType)) {
            return List.of(
                    new SubcategoryDef("ART", "Art"),
                    new SubcategoryDef("MUSIC", "Music"));
        } else {
            return List.of(
                    new SubcategoryDef("SPORTS", "Sports"),
                    new SubcategoryDef("CAMERAS", "Cameras"));
        }
    }

    private VBox buildSubcategoryRow(
            String displayName, List<ProductCardUiModel> cards, CarouselState carousel) {
        VBox wrap = new VBox();
        wrap.setSpacing(18.0);
        wrap.getStyleClass().add("gp-section-wrap");
        wrap.setPadding(new Insets(24.0, 0.0, 24.0, 0.0));

        Label title = new Label(displayName);
        title.getStyleClass().add("gp-section-title");

        Region divider = new Region();
        divider.setPrefHeight(1.0);
        divider.setPrefWidth(470.0);
        divider.getStyleClass().add("gp-section-divider");

        HBox carouselWrap = new HBox();
        carouselWrap.setAlignment(Pos.CENTER);
        carouselWrap.setSpacing(16.0);
        carouselWrap.getStyleClass().add("gp-cards-carousel-wrap");

        Button prevBtn = buildNavButton("fas-arrow-alt-circle-left");
        Button nextBtn = buildNavButton("fas-arrow-alt-circle-right");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(false);
        scrollPane.getStyleClass().add("gp-cards-scroll");
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        HBox cardsContainer = new HBox();
        cardsContainer.setSpacing(40.0);
        cardsContainer.setAlignment(Pos.TOP_LEFT);
        scrollPane.setContent(cardsContainer);

        HBox indicators = new HBox();
        indicators.setAlignment(Pos.CENTER);
        indicators.setSpacing(8.0);
        indicators.getStyleClass().add("gp-block-indicators");

        prevBtn.setOnAction(e -> moveToPrev(carousel));
        nextBtn.setOnAction(e -> moveToNext(carousel));

        carouselWrap.getChildren().addAll(prevBtn, scrollPane, nextBtn);
        wrap.getChildren().addAll(title, divider, carouselWrap, indicators);

        if (cards == null || cards.isEmpty()) {
            Label empty = new Label("No auctions available in this category.");
            empty.getStyleClass().add("gp-empty-state-label");
            cardsContainer.getChildren().add(empty);
            prevBtn.setDisable(true);
            nextBtn.setDisable(true);
            return wrap;
        }

        for (ProductCardUiModel card : cards) {
            if (card.isUpcoming()) {
                cardsContainer.getChildren().add(loadUpcomingCard(card));
            } else if (card.isEnded()) {
                cardsContainer.getChildren().add(loadEndedCard(card));
            } else {
                cardsContainer.getChildren().add(loadLiveCard(card));
            }
        }

        carousel.scrollPane = scrollPane;
        carousel.cardsContainer = cardsContainer;
        carousel.indicators = indicators;
        carousel.snapDelay = new PauseTransition(Duration.millis(150));
        carousel.snapDelay.setOnFinished(ev -> snapToNearest(carousel));

        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            double newWidth = newBounds.getWidth();
            if (carousel.lastLayoutWidth > 0
                    && Math.abs(newWidth - carousel.lastLayoutWidth) < 20.0) {
                return;
            }
            carousel.lastLayoutWidth = newWidth;
            updateLayout(carousel);
            snapToCurrent(carousel);
        });

        scrollPane.hvalueProperty().addListener((obs, oldV, newV) -> {
            if (carousel.programmaticScroll) {
                return;
            }
            carousel.snapDelay.playFromStart();
        });

        Platform.runLater(() -> {
            updateLayout(carousel);
            snapToCurrent(carousel);
        });

        return wrap;
    }

    private Button buildNavButton(String iconCode) {
        Button btn = new Button();
        btn.getStyleClass().add("gp-cards-nav-btn");
        btn.setFocusTraversable(false);
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(26);
        icon.getStyleClass().add("gp-cards-nav-icon");
        btn.setGraphic(icon);
        return btn;
    }

    private void updateLayout(CarouselState state) {
        double carouselWidth = resolveViewportWidth(state.scrollPane);
        if (carouselWidth <= 0) {
            return;
        }

        double spacing = state.cardsContainer.getSpacing();
        int total = state.cardsContainer.getChildren().size();
        if (total <= 0) {
            return;
        }

        int perBlock = Math.max(
                1, (int) Math.floor((carouselWidth + spacing) / (TARGET_CARD_WIDTH + spacing)));
        state.itemsPerBlock = perBlock;

        double cardWidth = (carouselWidth - spacing * (perBlock - 1)) / perBlock;
        for (Node card : state.cardsContainer.getChildren()) {
            if (card instanceof Region region) {
                region.setPrefWidth(cardWidth);
                region.setMinWidth(cardWidth);
                region.setMaxWidth(cardWidth);
            }
        }

        rebuildBlockStarts(state, total);
        rebuildIndicators(state);
        state.currentBlock = Math.min(state.currentBlock, Math.max(0, state.blockStarts.size() - 1));
        updateIndicatorHighlight(state);
    }

    private double resolveViewportWidth(ScrollPane scrollPane) {
        Node viewport = scrollPane.lookup(".viewport");
        if (viewport instanceof Region viewportRegion) {
            double width = viewportRegion.getWidth()
                    - viewportRegion.getInsets().getLeft()
                    - viewportRegion.getInsets().getRight();
            if (width > 0) {
                return width;
            }
        }
        return scrollPane.getWidth()
                - scrollPane.getInsets().getLeft()
                - scrollPane.getInsets().getRight();
    }

    private void rebuildBlockStarts(CarouselState state, int total) {
        state.blockStarts.clear();
        if (total <= 0) {
            return;
        }
        int maxStart = Math.max(0, total - state.itemsPerBlock);
        for (int start = 0; start <= maxStart; start += state.itemsPerBlock) {
            state.blockStarts.add(start);
        }
        if (state.blockStarts.isEmpty()
                || state.blockStarts.get(state.blockStarts.size() - 1) != maxStart) {
            state.blockStarts.add(maxStart);
        }
    }

    private void rebuildIndicators(CarouselState state) {
        state.indicators.getChildren().clear();
        for (int i = 0; i < state.blockStarts.size(); i++) {
            final int blockIdx = i;
            Region dot = new Region();
            dot.getStyleClass().add("gp-block-indicator");
            dot.setOnMouseClicked(ev -> {
                state.currentBlock = blockIdx;
                snapToCurrent(state);
            });
            state.indicators.getChildren().add(dot);
        }
    }

    private void snapToNearest(CarouselState state) {
        if (state.blockStarts.isEmpty()) {
            return;
        }
        double hValue = state.scrollPane.getHvalue();
        int nearest = 0;
        double nearestDist = Double.MAX_VALUE;
        for (int i = 0; i < state.blockStarts.size(); i++) {
            double blockH = toHValue(state, state.blockStarts.get(i));
            double dist = Math.abs(hValue - blockH);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = i;
            }
        }
        state.currentBlock = nearest;
        snapToCurrent(state);
    }

    private void snapToCurrent(CarouselState state) {
        if (state.blockStarts.isEmpty()) {
            return;
        }
        int startIndex = state.blockStarts.get(state.currentBlock);
        double hValue = toHValue(state, startIndex);
        state.programmaticScroll = true;
        state.scrollPane.setHvalue(hValue);
        state.programmaticScroll = false;
        updateIndicatorHighlight(state);
    }

    private double toHValue(CarouselState state, int itemStartIndex) {
        int maxStart = Math.max(0, state.cardsContainer.getChildren().size() - state.itemsPerBlock);
        if (maxStart == 0) {
            return 0.0;
        }
        return (double) itemStartIndex / maxStart;
    }

    private void updateIndicatorHighlight(CarouselState state) {
        for (int i = 0; i < state.indicators.getChildren().size(); i++) {
            Node dot = state.indicators.getChildren().get(i);
            if (i == state.currentBlock) {
                if (!dot.getStyleClass().contains("gp-block-indicator-active")) {
                    dot.getStyleClass().add("gp-block-indicator-active");
                }
            } else {
                dot.getStyleClass().remove("gp-block-indicator-active");
            }
        }
    }

    private void moveToPrev(CarouselState state) {
        if (state.blockStarts.isEmpty()) {
            return;
        }
        state.currentBlock = Math.max(0, state.currentBlock - 1);
        snapToCurrent(state);
    }

    private void moveToNext(CarouselState state) {
        if (state.blockStarts.isEmpty()) {
            return;
        }
        state.currentBlock = Math.min(state.blockStarts.size() - 1, state.currentBlock + 1);
        snapToCurrent(state);
    }

    private VBox loadLiveCard(ProductCardUiModel card) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(AUCTION_CARD_FXML));
        try {
            VBox root = loader.load();
            AuctionCardComponentController ctrl = loader.getController();
            ctrl.setData(card);
            return root;
        } catch (IOException e) {
            throw new SceneLoadException("Failed to load auction-card component", e);
        }
    }

    private VBox loadUpcomingCard(ProductCardUiModel card) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(UPCOMING_CARD_FXML));
        try {
            VBox root = loader.load();
            UpcomingAuctionCardComponentController ctrl = loader.getController();
            ctrl.setData(card);
            return root;
        } catch (IOException e) {
            throw new SceneLoadException("Failed to load upcoming-auction-card component", e);
        }
    }

    private VBox loadEndedCard(ProductCardUiModel card) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(ENDED_CARD_FXML));
        try {
            VBox root = loader.load();
            EndedAuctionCardComponentController ctrl = loader.getController();
            ctrl.setData(card);
            return root;
        } catch (IOException e) {
            throw new SceneLoadException("Failed to load ended-auction-card component", e);
        }
    }

    private static class CarouselState {
        private ScrollPane scrollPane;
        private HBox cardsContainer;
        private HBox indicators;
        private PauseTransition snapDelay;
        private final List<Integer> blockStarts = new ArrayList<>();
        private int itemsPerBlock = 1;
        private int currentBlock = 0;
        private boolean programmaticScroll;
        private double lastLayoutWidth = -1;
    }

    private record SubcategoryDef(String key, String displayName) {}
}
