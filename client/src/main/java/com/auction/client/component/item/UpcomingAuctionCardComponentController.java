package com.auction.client.component.item;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.auction.client.config.SceneRegistry;
import com.auction.client.dto.ProductCardUiModel;
import com.auction.client.scene.NavigationService;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class UpcomingAuctionCardComponentController {

    private static final long MAX_CLICK_DURATION_MILLIS = 250;

    private static final List<UpcomingAuctionCardViewData> FAKE_DATA = List.of(
        new UpcomingAuctionCardViewData("Item Image", "Vintage Rolex Submariner", "$12,600", "Starts at 02h 31m", "36 people are watching"),
        new UpcomingAuctionCardViewData("Item Image", "Hermes Birkin 30 Togo", "$8,900", "Starts at 11h 20m", "18 people are watching"),
        new UpcomingAuctionCardViewData("Item Image", "1968 Fender Telecaster", "$15,300", "Starts at 01d 03h", "52 people are watching")
    );

    @FXML
    private Label imageLabel;

    @FXML
    private Label timeStartLabel;

    @FXML
    private Label titleLabel;

    @FXML
    private Label priceLabel;

    @FXML
    private Label watchersLabel;

    private boolean hasExternalData;
    private long pressStartedAtNanos;

    @FXML
    private void handleMousePressed(MouseEvent event) {
        if (event != null && event.getButton() == MouseButton.PRIMARY) {
            pressStartedAtNanos = System.nanoTime();
        }
    }

    @FXML
    private void handleOpenProductDetail(MouseEvent event) {
        if (!isPrimaryClickOnly(event)) {
            return;
        }

        NavigationService.getInstance().navigateTo(SceneRegistry.PRODUCT_DETAIL_PAGE);
    }

    private boolean isPrimaryClickOnly(MouseEvent event) {
        long pressDurationMillis = (System.nanoTime() - pressStartedAtNanos) / 1_000_000L;
        return event != null
            && event.getButton() == MouseButton.PRIMARY
            && event.isStillSincePress()
            && pressDurationMillis <= MAX_CLICK_DURATION_MILLIS;
    }

    @FXML
    private void initialize() {
        if (!hasExternalData) {
            int randomIndex = ThreadLocalRandom.current().nextInt(FAKE_DATA.size());
            applyData(FAKE_DATA.get(randomIndex));
        }
    }

    public void setData(ProductCardUiModel model) {
        if (model == null) {
            return;
        }

        hasExternalData = true;
        applyData(new UpcomingAuctionCardViewData(
            "Item Image",
            model.title(),
            model.currentBid(),
            "Starts at " + model.timeLeft(),
            "36 people are watching"
        ));
    }

    public void useFakeData(int index) {
        int boundedIndex = Math.floorMod(index, FAKE_DATA.size());
        hasExternalData = true;
        applyData(FAKE_DATA.get(boundedIndex));
    }

    private void applyData(UpcomingAuctionCardViewData data) {
        imageLabel.setText(data.imageText());
        timeStartLabel.setText(data.timeStart());
        titleLabel.setText(data.title());
        priceLabel.setText(data.price());
        watchersLabel.setText(data.watchers());
    }

    private record UpcomingAuctionCardViewData(
        String imageText,
        String title,
        String price,
        String timeStart,
        String watchers
    ) {
    }
}
