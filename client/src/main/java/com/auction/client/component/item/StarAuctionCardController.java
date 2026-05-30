package com.auction.client.component.item;

import com.auction.client.config.SceneRegistry;
import com.auction.client.scene.NavigationService;
import com.auction.client.service.ImageLoader;
import com.auction.core.dto.auction.PublicAuctionDto;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

/**
 * Controller cho star-auction-card.fxml. Layout 50:50: bên trái là info text, bên phải là thumbnail
 * ảnh. Nhận dữ liệu qua {@link #setData(PublicAuctionDto)}.
 */
public class StarAuctionCardController {

    private static final long MAX_CLICK_DURATION_MILLIS = 250;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label priceCaptionLabel;
    @FXML private Label priceLabel;
    @FXML private Label sellerLabel;
    @FXML private Label timeLabel;
    @FXML private Label imagePlaceholderLabel;
    @FXML private StackPane imageContainer; // styleClass="sa-card-image-pane"

    private long pressStartedAtNanos;
    private Integer auctionId;
    private PublicAuctionDto currentDto;
    private Timeline cardTimeline;

    @FXML
    private void handleMousePressed(MouseEvent event) {
        if (event != null && event.getButton() == MouseButton.PRIMARY) {
            pressStartedAtNanos = System.nanoTime();
        }
    }

    @FXML
    private void handleOpenDetail(MouseEvent event) {
        if (!isPrimaryClickOnly(event)) {
            return;
        }
        if (auctionId != null) {
            NavigationService.getInstance()
                    .navigateTo(SceneRegistry.AUCTION_PAGE, Map.of("auctionId", auctionId));
        }
    }

    public Integer getAuctionId() {
        return auctionId;
    }

    public void updatePrice(double newPrice) {
        javafx.application.Platform.runLater(
                () -> {
                    if (currentDto != null) {
                        currentDto.setCurrentPrice(newPrice);
                    }
                    priceLabel.setText(String.format("$%,.2f", newPrice));
                });
    }

    /**
     * Populates the card with data from {@link PublicAuctionDto}. Called from carousel controller.
     */
    public void setData(PublicAuctionDto dto) {
        if (dto == null) {
            return;
        }

        this.currentDto = dto;
        this.auctionId = dto.getAuctionId();

        // Title
        titleLabel.setText(dto.getItemName() != null ? dto.getItemName() : "—");

        // Description (promoted or fallback text)
        String desc = dto.getPromotedDescription();
        descriptionLabel.setText(desc != null && !desc.isBlank() ? desc : "");
        descriptionLabel.setVisible(desc != null && !desc.isBlank());
        descriptionLabel.setManaged(desc != null && !desc.isBlank());

        // Price
        double price = dto.getCurrentPrice() != null ? dto.getCurrentPrice() : 0.0;
        priceLabel.setText(String.format("$%,.2f", price));

        // Seller
        String seller = dto.getSellerDisplayName();
        sellerLabel.setText(seller != null && !seller.isBlank() ? "by " + seller : "");

        // Time remaining and status views
        refreshTimeAndStatus();

        // Image — set via inline style on the StackPane (right half)
        applyThumbnail(dto.getThumbnailUrl());

        // Start countdown timeline for this card
        startTimeline();
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private void startTimeline() {
        if (cardTimeline != null) {
            cardTimeline.stop();
        }
        cardTimeline =
                new Timeline(
                        new KeyFrame(
                                javafx.util.Duration.seconds(1),
                                ev -> {
                                    refreshTimeAndStatus();
                                }));
        cardTimeline.setCycleCount(Timeline.INDEFINITE);
        cardTimeline.play();

        // Stop auto-scroll if carousel is detached from scene
        titleLabel
                .sceneProperty()
                .addListener(
                        (obs, oldScene, newScene) -> {
                            if (newScene == null && cardTimeline != null) {
                                cardTimeline.stop();
                            }
                        });
    }

    private void refreshTimeAndStatus() {
        if (currentDto == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = currentDto.getStartTime();
        LocalDateTime endTime = currentDto.getEndTime();
        String status = currentDto.getStatus();

        // Transition status dynamically in UI based on time
        if ("PENDING".equalsIgnoreCase(status)) {
            if (startTime != null && !now.isBefore(startTime)) {
                currentDto.setStatus("ACTIVE");
                status = "ACTIVE";
            }
        }
        if ("ACTIVE".equalsIgnoreCase(status)) {
            if (endTime != null && !now.isBefore(endTime)) {
                currentDto.setStatus("ENDED");
                status = "ENDED";
            }
        }

        // Update labels
        if ("PENDING".equalsIgnoreCase(status)) {
            priceCaptionLabel.setText("Starting Bid");
            if (startTime != null) {
                timeLabel.setText("Starts " + startTime.format(TIME_FMT));
            } else {
                timeLabel.setText("Coming soon");
            }
        } else if ("ENDED".equalsIgnoreCase(status) || (endTime != null && endTime.isBefore(now))) {
            priceCaptionLabel.setText("Final Price");
            timeLabel.setText("Ended");
            if (cardTimeline != null) {
                cardTimeline.stop();
            }
        } else {
            priceCaptionLabel.setText("Current Bid");
            if (endTime == null) {
                timeLabel.setText("—");
            } else {
                Duration remaining = Duration.between(now, endTime);
                long totalHours = remaining.toHours();
                if (totalHours >= 24) {
                    long days = totalHours / 24;
                    long hours = totalHours % 24;
                    timeLabel.setText(String.format("Ends after %dd %dh", days, hours));
                } else {
                    long minutes = remaining.toMinutesPart();
                    timeLabel.setText(String.format("Ends after %dh %dm", totalHours, minutes));
                }
            }
        }
    }

    /**
     * Set background image for the right pane. In mock mode, if thumbnailUrl is empty -> displays a
     * gradient placeholder.
     */
    private void applyThumbnail(String url) {
        if (imageContainer == null) {
            return;
        }
        if (imagePlaceholderLabel != null && !imagePlaceholderLabel.managedProperty().isBound()) {
            imagePlaceholderLabel.managedProperty().bind(imagePlaceholderLabel.visibleProperty());
        }
        // Star card image container is a bit larger, target width 450 is suitable
        ImageLoader.loadImage(url, imageContainer, imagePlaceholderLabel, 450);
    }

    private boolean isPrimaryClickOnly(MouseEvent event) {
        long pressDurationMillis = (System.nanoTime() - pressStartedAtNanos) / 1_000_000L;
        return event != null
                && event.getButton() == MouseButton.PRIMARY
                && event.isStillSincePress()
                && pressDurationMillis <= MAX_CLICK_DURATION_MILLIS;
    }
}
