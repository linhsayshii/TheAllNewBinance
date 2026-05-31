package com.auction.client.component.item;

import com.auction.client.config.SceneRegistry;
import com.auction.client.scene.NavigationService;
import com.auction.client.service.ImageLoader;
import com.auction.core.dto.auction.PublicAuctionDto;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * Controller for admin-auction-card.fxml. Similar to ProfileAuctionCardController but: — Receives a
 * {@link PublicAuctionDto} (from the admin all-auctions endpoint) — Shows a [Force Promote] button
 * when a Runnable callback is provided
 */
public class AdminAuctionCardController {

    private static final long MAX_CLICK_MS = 250;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @FXML private javafx.scene.layout.StackPane imageContainer;
    @FXML private Label imageLabel;
    @FXML private Label titleLabel;
    @FXML private Label sellerLabel;
    @FXML private Label priceCaptionLabel;
    @FXML private Label priceLabel;
    @FXML private Label badgeLabel;
    @FXML private Label timeLabel;
    @FXML private Button forcePromoteBtn;

    private long pressedAt;
    private Integer auctionId;
    private Runnable onForcePromote;

    // ── Mouse events ──────────────────────────────────────────────────────

    @FXML
    private void handleMousePressed(MouseEvent e) {
        if (e != null && e.getButton() == MouseButton.PRIMARY) {
            pressedAt = System.nanoTime();
        }
    }

    @FXML
    private void handleOpenAuction(MouseEvent e) {
        if (!isPrimaryClick(e)) {
            return;
        }
        if (auctionId != null) {
            NavigationService.getInstance()
                    .navigateTo(SceneRegistry.AUCTION_PAGE, Map.of("auctionId", auctionId));
        }
    }

    @FXML
    private void handleForcePromote() {
        if (onForcePromote != null) {
            onForcePromote.run();
        }
    }

    // ── Data binding ──────────────────────────────────────────────────────

    public void setData(PublicAuctionDto dto, Runnable onForcePromote) {
        if (dto == null) {
            return;
        }

        this.auctionId = dto.getAuctionId();
        this.onForcePromote = onForcePromote;

        ImageLoader.loadImage(dto.getThumbnailUrl(), imageContainer, imageLabel);

        titleLabel.setText(
                dto.getItemName() != null ? dto.getItemName() : "Auction #" + dto.getAuctionId());

        String seller = dto.getSellerDisplayName();
        sellerLabel.setText(seller != null && !seller.isBlank() ? "by " + seller : "");

        double price = dto.getCurrentPrice() != null ? dto.getCurrentPrice() : 0.0;
        priceCaptionLabel.setText("Current bid");
        priceLabel.setText(String.format("$%,.2f", price));

        timeLabel.setText(buildTimeLabel(dto));

        // Badge
        String status = dto.getStatus() != null ? dto.getStatus().toUpperCase() : "ACTIVE";
        badgeLabel.setText(status);
        badgeLabel.getStyleClass().removeIf(c -> c.startsWith("badge-"));
        badgeLabel
                .getStyleClass()
                .add(
                        switch (status) {
                            case "ACTIVE" -> "badge-live";
                            case "PENDING" -> "badge-pending";
                            case "ENDED" -> "badge-sold";
                            case "CANCELLED" -> "badge-cancelled";
                            default -> "badge-live";
                        });

        // Force Promote button
        boolean showBtn = (onForcePromote != null);
        forcePromoteBtn.setVisible(showBtn);
        forcePromoteBtn.setManaged(showBtn);

        if (showBtn && Boolean.TRUE.equals(dto.getIsFeatured())) {
            forcePromoteBtn.setText("★  Featured");
            forcePromoteBtn.getStyleClass().add("profile-card-promote-btn-active");
            forcePromoteBtn.setDisable(true);
        } else {
            forcePromoteBtn.setText("★  Force Promote");
            forcePromoteBtn.getStyleClass().remove("profile-card-promote-btn-active");
            forcePromoteBtn.setDisable(false);
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private String buildTimeLabel(PublicAuctionDto dto) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = dto.getEndTime();
        if ("PENDING".equalsIgnoreCase(dto.getStatus())) {
            LocalDateTime st = dto.getStartTime();
            return st != null ? "Starts " + st.format(TIME_FMT) : "Coming soon";
        }
        if (endTime == null) {
            return "—";
        }
        if (endTime.isBefore(now)) {
            return "Ended";
        }
        Duration rem = Duration.between(now, endTime);
        long h = rem.toHours();
        if (h >= 24) {
            return String.format("Ends after %dd %dh", h / 24, h % 24);
        }
        return String.format("Ends after %dh %dm", h, rem.toMinutesPart());
    }

    private boolean isPrimaryClick(MouseEvent e) {
        long ms = (System.nanoTime() - pressedAt) / 1_000_000L;
        return e != null
                && e.getButton() == MouseButton.PRIMARY
                && e.isStillSincePress()
                && ms <= MAX_CLICK_MS;
    }
}
