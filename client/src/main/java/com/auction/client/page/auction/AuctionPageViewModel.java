package com.auction.client.page.auction;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.products.Item;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AuctionPageViewModel {

    public String productTitle() {
        return title.get();
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String safeUpper(String value, String fallback) {
        return safe(value, fallback).toUpperCase();
    }

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");

    private final IntegerProperty auctionId = new SimpleIntegerProperty(0);
    private final IntegerProperty sellerId = new SimpleIntegerProperty(1); // Temporary dummy value
    private final StringProperty category = new SimpleStringProperty("CATEGORY");
    private final StringProperty title = new SimpleStringProperty("Product detail placeholder");
    private final StringProperty description = new SimpleStringProperty("No description");
    private final StringProperty imageUrl = new SimpleStringProperty("Image Url");
    private final StringProperty sellerName = new SimpleStringProperty("Unknown Seller");
    private final StringProperty sellerEmail = new SimpleStringProperty("");
    private final StringProperty sellerJoinDate = new SimpleStringProperty("");
    private final StringProperty currentBidDisplay = new SimpleStringProperty("\\$0.00");
    private final StringProperty bidderCountText = new SimpleStringProperty("0 people bidding");
    private final StringProperty countdownText = new SimpleStringProperty("00d 00h 00m 00s");
    private final StringProperty loginPrompt =
            new SimpleStringProperty("Please log in or sign up to place a bid");

    private final IntegerProperty winnerId = new SimpleIntegerProperty(0);
    private final ObjectProperty<LocalDateTime> startTime =
            new SimpleObjectProperty<>(LocalDateTime.now());
    private final ObjectProperty<LocalDateTime> endTime =
            new SimpleObjectProperty<>(LocalDateTime.now());
    private final ObjectProperty<Auction.Status> status =
            new SimpleObjectProperty<>(Auction.Status.ACTIVE);
    private final DoubleProperty currentBidAmount = new SimpleDoubleProperty(0.0);
    private final DoubleProperty bidIncrement = new SimpleDoubleProperty(1.0);
    private final BooleanProperty biddingEnabled = new SimpleBooleanProperty(true);

    private final ObservableList<Bid> bids = FXCollections.observableArrayList();

    public void setAuctionId(int id) {
        auctionId.set(id);
    }

    public int getWinnerId() {
        return winnerId.get();
    }

    public IntegerProperty winnerIdProperty() {
        return winnerId;
    }

    public ObjectProperty<LocalDateTime> startTimeProperty() {
        return startTime;
    }

    public ObjectProperty<Auction.Status> statusProperty() {
        return status;
    }

    public StringProperty sellerEmailProperty() {
        return sellerEmail;
    }

    public StringProperty sellerJoinDateProperty() {
        return sellerJoinDate;
    }

    public void applyAuctionData(
            Auction auction,
            Item item,
            com.auction.core.users.User sellerUser,
            Integer currentBidderId,
            List<Bid> bidHistory) {
        if (auction != null) {
            auctionId.set(auction.getId() != null ? auction.getId() : 0);
            currentBidAmount.set(
                    auction.getCurrentPrice() != null ? auction.getCurrentPrice() : 0.0);
            bidIncrement.set(auction.getBidIncrement() != null ? auction.getBidIncrement() : 1.0);
            startTime.set(
                    auction.getStartTime() != null ? auction.getStartTime() : LocalDateTime.now());
            endTime.set(auction.getEndTime() != null ? auction.getEndTime() : LocalDateTime.now());
            status.set(auction.getStatus() != null ? auction.getStatus() : Auction.Status.ACTIVE);
            winnerId.set(auction.getWinnerId() != null ? auction.getWinnerId() : 0);

            boolean active = auction.getStatus() == Auction.Status.ACTIVE;
            if (auction.getEndTime() != null
                    && auction.getEndTime().isBefore(LocalDateTime.now())) {
                active = false;
                status.set(Auction.Status.ENDED);
            }
            biddingEnabled.set(active);
        }

        if (item != null) {
            category.set(
                    item.getCategory() != null
                            ? item.getCategory().getDisplayName().toUpperCase()
                            : "CATEGORY");
            title.set(safe(item.getName(), "Product detail placeholder"));
            description.set(safe(item.getDescription(), "No description"));
            imageUrl.set(
                    item.getImageUrl() != null && !item.getImageUrl().isBlank()
                            ? item.getImageUrl()
                            : "Item Image");
            sellerId.set(item.getSellerId() != null ? item.getSellerId() : 0);
        }

        if (sellerUser != null) {
            String name = safe(sellerUser.getFullName(), safe(sellerUser.getUsername(), "Unknown Seller"));
            sellerName.set(name);
            sellerEmail.set(safe(sellerUser.getEmail(), ""));
            sellerJoinDate.set(sellerUser.getCreatedAt() != null
                    ? "Member since " + sellerUser.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"))
                    : "");
        } else {
            sellerName.set("Unknown Seller");
            sellerEmail.set("");
            sellerJoinDate.set("");
        }

        updateCurrentBidDisplay(currentBidAmount.get());
        if (bidHistory != null) {
            setBids(bidHistory);
        }
    }

    public void setBids(List<Bid> bidHistory) {
        bids.clear();
        if (bidHistory == null || bidHistory.isEmpty()) {
            bidderCountText.set("0 people bidding");
            return;
        }

        bidHistory.stream()
                .sorted(
                        Comparator.comparing(
                                        Bid::getCreatedAt,
                                        Comparator.nullsLast(Comparator.naturalOrder()))
                                .reversed())
                .forEach(bids::add);

        bidderCountText.set(bids.size() + " people bidding");
        Bid latest = bids.get(0);
        if (latest.getAmount() != null) {
            updateCurrentBidDisplay(latest.getAmount());
        }
    }

    public void updateCurrentBidDisplay(double amount) {
        currentBidAmount.set(amount);
        currentBidDisplay.set("$" + MONEY_FORMAT.format(amount));
    }

    public void updateCountdown(LocalDateTime now) {
        if (status.get() == Auction.Status.PENDING) {
            LocalDateTime start = startTime.get();
            if (start != null && !now.isBefore(start)) {
                status.set(Auction.Status.ACTIVE);
                biddingEnabled.set(true);
            }
        }

        if (status.get() == Auction.Status.ACTIVE) {
            LocalDateTime end = endTime.get();
            if (end != null && !now.isBefore(end)) {
                status.set(Auction.Status.ENDED);
                biddingEnabled.set(false);
            }
        }

        if (status.get() == Auction.Status.PENDING) {
            LocalDateTime start = startTime.get();
            if (start == null) {
                countdownText.set("00d 00h 00m 00s");
                return;
            }
            Duration remaining = Duration.between(now, start);
            if (remaining.isNegative() || remaining.isZero()) {
                countdownText.set("00d 00h 00m 00s");
                return;
            }
            long totalSeconds = remaining.getSeconds();
            long days = totalSeconds / 86_400;
            long hours = (totalSeconds % 86_400) / 3_600;
            long minutes = (totalSeconds % 3_600) / 60;
            long seconds = totalSeconds % 60;
            countdownText.set(
                    String.format("%02dd %02dh %02dm %02ds", days, hours, minutes, seconds));
        } else if (status.get() == Auction.Status.ACTIVE) {
            LocalDateTime end = endTime.get();
            if (end == null) {
                countdownText.set("00d 00h 00m 00s");
                return;
            }
            Duration remaining = Duration.between(now, end);
            if (remaining.isNegative() || remaining.isZero()) {
                countdownText.set("Auction ended");
                biddingEnabled.set(false);
                return;
            }
            long totalSeconds = remaining.getSeconds();
            long days = totalSeconds / 86_400;
            long hours = (totalSeconds % 86_400) / 3_600;
            long minutes = (totalSeconds % 3_600) / 60;
            long seconds = totalSeconds % 60;
            countdownText.set(
                    String.format("%02dd %02dh %02dm %02ds", days, hours, minutes, seconds));
        } else {
            countdownText.set("Auction ended");
            biddingEnabled.set(false);
        }
    }

    public double minimumBidAmount() {
        return currentBidAmount.get() + bidIncrement.get();
    }

    public Integer getAuctionId() {
        return auctionId.get();
    }

    public IntegerProperty auctionIdProperty() {
        return auctionId;
    }

    public Integer getSellerId() {
        return sellerId.get();
    }

    public StringProperty categoryProperty() {
        return category;
    }

    public StringProperty titleProperty() {
        return title;
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public StringProperty sellerNameProperty() {
        return sellerName;
    }

    public StringProperty currentBidDisplayProperty() {
        return currentBidDisplay;
    }

    public StringProperty bidderCountTextProperty() {
        return bidderCountText;
    }

    public StringProperty countdownTextProperty() {
        return countdownText;
    }

    public StringProperty loginPromptProperty() {
        return loginPrompt;
    }

    public ObjectProperty<LocalDateTime> endTimeProperty() {
        return endTime;
    }

    public ObservableList<Bid> bids() {
        return bids;
    }

    public BooleanProperty biddingEnabledProperty() {
        return biddingEnabled;
    }

    public boolean isBiddingEnabled() {
        return biddingEnabled.get();
    }

    public String imageUrl() {
        return imageUrl.get();
    }

    public StringProperty imageUrlProperty() {
        return imageUrl;
    }
}
