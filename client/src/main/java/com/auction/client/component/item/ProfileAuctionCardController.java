package com.auction.client.component.item;

import com.auction.client.config.SceneRegistry;
import com.auction.client.dto.ProfileAuctionCardUiModel;
import com.auction.client.scene.NavigationService;
import com.auction.client.service.ImageLoader;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * Controller for the compact profile auction card (profile-auction-card.fxml).
 *
 * <p>Differences from {@link AuctionCardComponentController} (general page): — Uses {@link
 * ProfileAuctionCardUiModel} instead of ProductCardUiModel — Sets a coloured status badge with a
 * dynamic CSS style class — No progress bar — Optional [Promote] button for seller's own listings
 * (showPromoteButton=true)
 */
public class ProfileAuctionCardController {

    private static final long MAX_CLICK_DURATION_MILLIS = 250;

    @FXML private javafx.scene.layout.VBox cardRoot;
    @FXML private javafx.scene.layout.StackPane imageContainer;
    @FXML private Label imageLabel;
    @FXML private Label titleLabel;
    @FXML private Label priceCaptionLabel;
    @FXML private Label priceLabel;
    @FXML private Label badgeLabel;
    @FXML private Label timeLabel;
    @FXML private Button promoteBtn;

    private long pressStartedAtNanos;
    private Integer auctionId;

    /** Callback set by PersonalProfileController to open the Promote modal */
    private Runnable onPromoteClick;

    // ── Mouse events ──────────────────────────────────────────────────────

    @FXML
    private void handleMousePressed(MouseEvent event) {
        if (event != null && event.getButton() == MouseButton.PRIMARY) {
            pressStartedAtNanos = System.nanoTime();
        }
    }

    @FXML
    private void handleOpenAuction(MouseEvent event) {
        if (!isPrimaryClickOnly(event)) {
            return;
        }
        if (auctionId != null) {
            NavigationService.getInstance()
                    .navigateTo(SceneRegistry.AUCTION_PAGE, Map.of("auctionId", auctionId));
        }
    }

    @FXML
    private void handlePromote() {
        if (onPromoteClick != null) {
            onPromoteClick.run();
        }
    }

    // ── Data binding ──────────────────────────────────────────────────────

    /**
     * Populates the card with data from a {@link ProfileAuctionCardUiModel}. Must be called after
     * the FXML has been loaded (i.e., from the parent controller that loads the card via
     * FXMLLoader).
     *
     * @param model card data
     * @param onPromote callback to run when [Promote] is clicked; if null the button stays hidden
     */
    public void setData(ProfileAuctionCardUiModel model, Runnable onPromote) {
        if (model == null) {
            return;
        }

        this.auctionId = model.auctionId();
        this.onPromoteClick = onPromote;

        ImageLoader.loadImage(model.imageUrl(), imageContainer, imageLabel);

        titleLabel.setText(model.title() != null ? model.title() : "Untitled");
        String caption = "Current bid";
        if (model.badgeLabel() != null) {
            String badge = model.badgeLabel().toUpperCase();
            switch (badge) {
                case "LIVE", "WINNING", "OUTBID" -> caption = "Current bid";
                case "PENDING" -> caption = "Opening bid";
                case "WON", "SOLD", "UNSOLD", "CANCELLED" -> caption = "Final bid";
            }
        }
        priceCaptionLabel.setText(caption);
        priceLabel.setText(model.price() != null ? model.price() : "$—");
        timeLabel.setText(model.timeInfo() != null ? model.timeInfo() : "");

        // Apply badge
        badgeLabel.setText(model.badgeLabel() != null ? model.badgeLabel() : "");
        badgeLabel.getStyleClass().removeIf(sc -> sc.startsWith("badge-"));
        if (model.badgeStyleClass() != null && !model.badgeStyleClass().isBlank()) {
            badgeLabel.getStyleClass().add(model.badgeStyleClass());
        }

        // Apply card-level status class for dynamic styling (similar to general page cards)
        if (cardRoot != null) {
            cardRoot.getStyleClass().removeAll("profile-card-live", "profile-card-pending", "profile-card-ended");
            String badge = model.badgeLabel() != null ? model.badgeLabel().toUpperCase() : "";
            switch (badge) {
                case "LIVE", "WINNING", "OUTBID" -> cardRoot.getStyleClass().add("profile-card-live");
                case "PENDING" -> cardRoot.getStyleClass().add("profile-card-pending");
                case "WON", "SOLD", "UNSOLD", "CANCELLED" -> cardRoot.getStyleClass().add("profile-card-ended");
                default -> cardRoot.getStyleClass().add("profile-card-live");
            }
        }

        // Promote button — show only when callback is provided
        boolean showPromote = (onPromote != null);
        promoteBtn.setVisible(showPromote);
        promoteBtn.setManaged(showPromote);

        // If already featured → change text to "★ Featured" (disabled)
        if (showPromote && model.isFeatured()) {
            promoteBtn.setText("★  Featured");
            promoteBtn.getStyleClass().add("profile-card-promote-btn-active");
            promoteBtn.setDisable(true);
        } else {
            promoteBtn.setText("★  Promote");
            promoteBtn.getStyleClass().remove("profile-card-promote-btn-active");
            promoteBtn.setDisable(false);
        }
    }

    /** Legacy overload — no Promote button. */
    public void setData(ProfileAuctionCardUiModel model) {
        setData(model, null);
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private boolean isPrimaryClickOnly(MouseEvent event) {
        long pressDurationMillis = (System.nanoTime() - pressStartedAtNanos) / 1_000_000L;
        return event != null
                && event.getButton() == MouseButton.PRIMARY
                && event.isStillSincePress()
                && pressDurationMillis <= MAX_CLICK_DURATION_MILLIS;
    }
}
