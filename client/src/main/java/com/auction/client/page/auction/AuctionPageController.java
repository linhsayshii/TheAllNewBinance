package com.auction.client.page.auction;

import java.net.URL;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.auction.client.config.SceneRegistry;
import com.auction.client.scene.DataReceivable;
import com.auction.client.scene.LifecycleAwareController;
import com.auction.client.scene.NavigationService;
import com.auction.client.service.ImageLoader;
import com.auction.client.service.NetworkService;
import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.dto.auction.GetAuctionDetailsRequest;
import com.auction.core.dto.bid.PlaceBid;
import com.auction.core.protocol.EventType;
import com.auction.core.utils.JsonMapper;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class AuctionPageController implements Initializable, LifecycleAwareController, DataReceivable {

    private static final String HANDLER_ID = "AUCTION_PAGE";
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DAY_TIME_FORMAT = DateTimeFormatter.ofPattern("EEEE HH:mm");

    @FXML private LineChart<String, Number> bidHistoryChart;
    @FXML private CategoryAxis xAxisTime;
    @FXML private NumberAxis yAxisPrice;
    @FXML private Label categoryLabel;
    @FXML private Label titleLabel;
    @FXML private javafx.scene.layout.StackPane imageContainer;
    @FXML private Label imageLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label statusTimePrefixLabel;
    @FXML private Label dayLabel;
    @FXML private Label countdownDaysLabel;
    @FXML private Label countdownHoursLabel;
    @FXML private Label countdownMinutesLabel;
    @FXML private Label countdownSecondsLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label bidderCountLabel;
    @FXML private Label sellerNameLabel;
    @FXML private Label loginPromptLabel;
    @FXML private TextField bidAmountInput;
    @FXML private Button placeBidButton;
    @FXML private ListView<String> bidHistoryList;
    @FXML private Label priceCaptionLabel;
    @FXML private javafx.scene.layout.VBox biddingInputArea;
    @FXML private javafx.scene.layout.VBox winnerBox;
    @FXML private Label winnerLabel;
    @FXML private javafx.scene.layout.StackPane loginPromptBox;

    private final AuctionPageViewModel viewModel = new AuctionPageViewModel();
    private Timeline countdownTimeline;
    private boolean networkReady;

    @Override
    public void onDataReceived(Map<String, Object> data) {
        if (data == null) {
            return;
        }
        Object auctionIdObj = data.get("auctionId");
        if (auctionIdObj instanceof Number) {
            int auctionId = ((Number) auctionIdObj).intValue();
            viewModel.setAuctionId(auctionId);
            if (networkReady && auctionId > 0) {
                fetchAuctionDetailsFromServer(auctionId);
                fetchBidHistoryFromServer(auctionId);
            }
        }
    }

    @FXML
    private void handleGoToGeneral() {
        NavigationService.getInstance().navigateTo(SceneRegistry.GENERAL_PAGE);
    }

    @FXML
    private void handleGoToLogin() {
        NavigationService.getInstance().openPopup(SceneRegistry.LOGIN_CARD);
    }

    @FXML
    private void handleGoToRegister() {
        NavigationService.getInstance().openPopup(SceneRegistry.REGISTER_CARD);
    }

    @FXML
    private void handleGoToProfile() {
        NavigationService.getInstance().navigateTo(SceneRegistry.PROFILE_PAGE);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupBindings();
        setupChart();
        startCountdownTicker();
        registerNetworkHandlers();
    }

    // ---- Network: Auction Details ----

    private void fetchAuctionDetailsFromServer(int auctionId) {
        GetAuctionDetailsRequest request = new GetAuctionDetailsRequest(auctionId);
        String correlationId = NetworkService.getInstance().sendRequest(EventType.GET_AUCTION_DETAILS, request);
        NetworkService.getInstance().addCorrelationHandler(correlationId, this::handleAuctionDetailsResponse);
    }

    private void handleAuctionDetailsResponse(String rawJson) {
        try {
            Map<?, ?> response = JsonMapper.fromJson(rawJson, Map.class);
            if (response == null) {
                return;
            }
            Object success = response.get("success");
            if (!(success instanceof Boolean) || !((Boolean) success)) {
                System.err.println("Failed to get auction details: " + response.get("message"));
                return;
            }
            Object data = response.get("data");
            if (data == null) {
                return;
            }
            String dataJson = JsonMapper.toJson(data);
            com.auction.core.dto.auction.AuctionDetailsDto dto = JsonMapper.fromJson(dataJson, com.auction.core.dto.auction.AuctionDetailsDto.class);
            if (dto != null && dto.getAuction() != null) {
                Platform.runLater(() -> {
                    String sellerName = dto.getSeller() != null ? (dto.getSeller().getFullName() != null && !dto.getSeller().getFullName().isBlank() ? dto.getSeller().getFullName() : dto.getSeller().getUsername()) : "Unknown Seller";
                    viewModel.applyAuctionData(dto.getAuction(), dto.getItem(), sellerName, null, null);
                    updateStatusViews();
                });
            }
        } catch (Exception e) {
            System.err.println("Error processing auction details response: " + e.getMessage());
        }
    }

    // ---- Network: Bid History ----

    private void handleBidHistoryResponse(String rawJson) {
        try {
            Map<?, ?> response = JsonMapper.fromJson(rawJson, Map.class);
            if (response == null || !response.containsKey("data") || !(response.get("data") instanceof List)) {
                return;
            }
            String dataJson = JsonMapper.toJson(response.get("data"));
            Bid[] bidsArray = JsonMapper.fromJson(dataJson, Bid[].class);
            if (bidsArray != null) {
                List<Bid> bids = Arrays.asList(bidsArray);
                Platform.runLater(() -> updateBidViews(bids));
            }
        } catch (Exception e) {
            System.err.println("Error processing socket response in AuctionPage: " + e.getMessage());
        }
    }

    private void handlePlaceBidResponse(String rawJson) {
        try {
            Map<?, ?> response = JsonMapper.fromJson(rawJson, Map.class);
            Object success = response != null ? response.get("success") : null;
            if (!(success instanceof Boolean) || !((Boolean) success)) {
                return;
            }

            Object data = response.get("data");
            if (data == null) {
                return;
            }

            Bid incomingBid = JsonMapper.fromJson(JsonMapper.toJson(data), Bid.class);
            if (incomingBid == null) {
                return;
            }
            Platform.runLater(() -> mergeSingleBid(incomingBid));
        } catch (Exception e) {
            System.err.println("Error processing place bid response in AuctionPage: " + e.getMessage());
        }
    }

    private void fetchBidHistoryFromServer(int auctionId) {
        Integer currentUserId = com.auction.client.service.UserSessionService.getInstance().isAuthenticated() 
            ? com.auction.client.service.UserSessionService.getInstance().getCurrentUser().getId() 
            : null;
            
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("auctionId", auctionId);
        if (currentUserId != null) {
            payload.put("userId", currentUserId);
        }
        NetworkService.getInstance().sendRequest(EventType.GET_BIDS_BY_AUCTION_ID, payload);
    }

    public void drawChart(List<Bid> bids) {
        bidHistoryChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Lịch sử giá");

        List<Bid> sorted = bids == null ? List.of() : bids.stream()
            .sorted(Comparator.comparing(Bid::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        for (Bid bid : sorted) {
            String timeLabel = bid.getCreatedAt() != null
                ? bid.getCreatedAt().format(TIME_FORMAT) : "Unknown";
            series.getData().add(new XYChart.Data<>(timeLabel, bid.getAmount()));
        }
        bidHistoryChart.getData().add(series);
    }

    @FXML
    private void handlePlaceBid() {
        if (!viewModel.isBiddingEnabled()) {
            return;
        }

        if (!com.auction.client.service.UserSessionService.getInstance().isAuthenticated()) {
            showInfo("Login Required", "Please log in to place a bid.");
            return;
        }
        
        Integer currentUserId = com.auction.client.service.UserSessionService.getInstance().getCurrentUser().getId();

        Double amount = parseBidInput();
        if (amount == null) {
            showInfo("Invalid amount", "Please enter a valid numeric bid amount.");
            return;
        }

        double minimum = viewModel.minimumBidAmount();
        if (amount < minimum) {
            showInfo("Bid too low", "Minimum required bid is $" + MONEY_FORMAT.format(minimum));
            return;
        }

        if (!networkReady) {
            showInfo("Network unavailable", "Cannot place bid because server connection is unavailable.");
            return;
        }

        PlaceBid payload = new PlaceBid(viewModel.getAuctionId(), currentUserId, amount);
        NetworkService.getInstance().sendRequest(EventType.PLACE_BID, payload);
    }

    @FXML
    private void handleSetAutoBid() {
        showInfo("Auto Bid", "Auto Bid popup will be implemented next. Local state strategy is ready.");
    }

    private void setupBindings() {
        categoryLabel.textProperty().bind(viewModel.categoryProperty());
        viewModel.titleProperty().addListener((obs, oldVal, newVal) -> updateTitleText());
        descriptionLabel.textProperty().bind(viewModel.descriptionProperty());

        viewModel.imageUrlProperty().addListener((obs, oldVal, newVal) -> {
            ImageLoader.loadImage(newVal, imageContainer, imageLabel, 800);
            updateStatusViews();
        });
        ImageLoader.loadImage(viewModel.imageUrl(), imageContainer, imageLabel, 800);

        currentBidLabel.textProperty().bind(viewModel.currentBidDisplayProperty());
        bidderCountLabel.textProperty().bind(viewModel.bidderCountTextProperty());
        sellerNameLabel.textProperty().bind(viewModel.sellerNameProperty());
        loginPromptLabel.textProperty().bind(viewModel.loginPromptProperty());

        bidAmountInput.disableProperty().bind(viewModel.biddingEnabledProperty().not());
        placeBidButton.disableProperty().bind(viewModel.biddingEnabledProperty().not());

        // Listen to biddingEnabled property changes to update status view dynamically
        viewModel.biddingEnabledProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(this::updateStatusViews);
        });

        // Listen to status property changes to update status view dynamically
        viewModel.statusProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                updateStatusViews();
                updateTitleText();
            });
        });

        // Current user session changes to show/hide login prompt box
        com.auction.client.service.UserSessionService.getInstance().currentUserProperty().addListener((obs, oldUser, newUser) -> {
            boolean loggedIn = (newUser != null);
            loginPromptBox.setVisible(!loggedIn);
            loginPromptBox.setManaged(!loggedIn);
        });
        boolean loggedIn = com.auction.client.service.UserSessionService.getInstance().isAuthenticated();
        loginPromptBox.setVisible(!loggedIn);
        loginPromptBox.setManaged(!loggedIn);

        // Update bid amount input prompt text dynamically
        viewModel.currentBidDisplayProperty().addListener((obs, oldVal, newVal) -> {
            updateBidAmountPromptText();
        });
        updateBidAmountPromptText();

        // Initial status view update
        updateStatusViews();
    }

    private void setupChart() {
        bidHistoryChart.setCreateSymbols(true);
        bidHistoryChart.getStyleClass().add("bid-history-chart");
    }

    private void registerNetworkHandlers() {
        try {
            NetworkService.getInstance().getClient()
                .addResponseHandler(EventType.GET_BIDS_BY_AUCTION_ID, HANDLER_ID, this::handleBidHistoryResponse);
            NetworkService.getInstance().getClient()
                .addResponseHandler(EventType.PLACE_BID, HANDLER_ID, this::handlePlaceBidResponse);
            networkReady = true;
        } catch (IllegalStateException ex) {
            networkReady = false;
        }
    }

    private void startCountdownTicker() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        countdownTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
            LocalDateTime now = LocalDateTime.now();
            viewModel.updateCountdown(now);
            updateCountdownLabels(now);
            if (viewModel.statusProperty().get() == Auction.Status.ENDED && countdownTimeline != null) {
                countdownTimeline.stop();
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
        // Initial update
        updateCountdownLabels(LocalDateTime.now());
    }

    @FXML
    private void handleGoToSellerProfile() {
        NavigationService.getInstance().navigateTo(SceneRegistry.PUBLIC_SELLER_PAGE,
                java.util.Map.of("sellerId", viewModel.getSellerId()));
    }

    private void updateCountdownLabels(LocalDateTime now) {
        Auction.Status status = viewModel.statusProperty().get();
        if (status == Auction.Status.PENDING) {
            statusTimePrefixLabel.setText("Start At");
            LocalDateTime start = viewModel.startTimeProperty().get();
            if (start != null) {
                dayLabel.setText(start.format(DAY_TIME_FORMAT));
                Duration remaining = Duration.between(now, start);
                if (remaining.isNegative() || remaining.isZero()) {
                    setCountdownZero();
                } else {
                    setCountdownDuration(remaining);
                }
            } else {
                dayLabel.setText("—");
                setCountdownZero();
            }
        } else if (status == Auction.Status.ACTIVE) {
            statusTimePrefixLabel.setText("End At");
            LocalDateTime end = viewModel.endTimeProperty().get();
            if (end != null) {
                dayLabel.setText(end.format(DAY_TIME_FORMAT));
                Duration remaining = Duration.between(now, end);
                if (remaining.isNegative() || remaining.isZero()) {
                    setCountdownZero();
                } else {
                    setCountdownDuration(remaining);
                }
            } else {
                dayLabel.setText("—");
                setCountdownZero();
            }
        } else { // ENDED or CANCELLED
            statusTimePrefixLabel.setText("End At");
            LocalDateTime end = viewModel.endTimeProperty().get();
            if (end != null) {
                dayLabel.setText(end.format(DAY_TIME_FORMAT));
            } else {
                dayLabel.setText("—");
            }
            setCountdownZero();
        }
    }

    private void setCountdownZero() {
        countdownDaysLabel.setText("00");
        countdownHoursLabel.setText("00");
        countdownMinutesLabel.setText("00");
        countdownSecondsLabel.setText("00");
    }

    private void setCountdownDuration(Duration duration) {
        long totalSecs = duration.getSeconds();
        long days = totalSecs / 86_400;
        long hours = (totalSecs % 86_400) / 3_600;
        long minutes = (totalSecs % 3_600) / 60;
        long seconds = totalSecs % 60;
        countdownDaysLabel.setText(String.format("%02d", days));
        countdownHoursLabel.setText(String.format("%02d", hours));
        countdownMinutesLabel.setText(String.format("%02d", minutes));
        countdownSecondsLabel.setText(String.format("%02d", seconds));
    }

    private void updateBidViews(List<Bid> bids) {
        viewModel.setBids(bids);
        drawChart(viewModel.bids());
        refreshBidHistoryList(viewModel.bids());
    }

    private void mergeSingleBid(Bid bid) {
        List<Bid> merged = new ArrayList<>(viewModel.bids());
        merged.add(bid);
        updateBidViews(merged);
        bidAmountInput.clear();
    }

    private void refreshBidHistoryList(List<Bid> bids) {
        List<String> rows = bids.stream().map(bid -> {
            String time = bid.getCreatedAt() == null ? "Unknown" : bid.getCreatedAt().format(TIME_FORMAT);
            String bidder = bid.getBidderId() == null ? "#-" : "#" + bid.getBidderId();
            String amount = "$" + MONEY_FORMAT.format(bid.getAmount() == null ? 0.0 : bid.getAmount());
            return time + "  |  Bidder " + bidder + "  |  " + amount;
        }).toList();
        bidHistoryList.getItems().setAll(rows);
    }

    private Double parseBidInput() {
        String raw = bidAmountInput.getText();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            String normalized = raw.replace("$", "").replace(",", "").trim();
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void updateImageContainer(String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank() && !"Item Image".equals(imageUrl)) {
            imageLabel.setVisible(false);
            String bgValue = "-fx-background-image: url('" + imageUrl + "'); " +
                             "-fx-background-size: cover; " +
                             "-fx-background-position: center center;";
            imageContainer.setStyle(bgValue);
        } else {
            imageLabel.setVisible(true);
            imageLabel.setText("Item Image");
            imageContainer.setStyle("");
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private void updateTitleText() {
        Auction.Status status = viewModel.statusProperty().get();
        String titleText = viewModel.productTitle();
        if (titleText == null) {
            titleText = "";
        }
        
        // Strip any existing prefixes first
        String cleanTitle = titleText;
        if (cleanTitle.startsWith("[ENDED] ")) {
            cleanTitle = cleanTitle.substring(8);
        } else if (cleanTitle.startsWith("[UPCOMING] ")) {
            cleanTitle = cleanTitle.substring(11);
        }

        if (status == Auction.Status.PENDING) {
            titleLabel.setText("[UPCOMING] " + cleanTitle);
        } else if (status == Auction.Status.ENDED || status == Auction.Status.CANCELLED) {
            titleLabel.setText("[ENDED] " + cleanTitle);
        } else {
            titleLabel.setText(cleanTitle);
        }
    }

    private void updateBidAmountPromptText() {
        double minimum = viewModel.minimumBidAmount();
        bidAmountInput.setPromptText("$" + MONEY_FORMAT.format(minimum));
    }

    private void updateStatusViews() {
        Auction.Status status = viewModel.statusProperty().get();

        // 1. Remove overlays on image container
        imageContainer.getChildren().removeIf(node -> "ended-overlay-label".equals(node.getId()) || "upcoming-overlay-label".equals(node.getId()));

        if (status == Auction.Status.PENDING) {
            Label upcomingOverlay = new Label("UPCOMING");
            upcomingOverlay.setId("upcoming-overlay-label");
            upcomingOverlay.setStyle("-fx-background-color: transparent; " +
                                  "-fx-text-fill: white; " +
                                  "-fx-font-family: 'Montserrat'; " +
                                  "-fx-font-weight: 800; " +
                                  "-fx-font-size: 26px; " +
                                  "-fx-padding: 12 30 12 30; " +
                                  "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 0);");
            imageContainer.getChildren().add(upcomingOverlay);

            // Update title text to include [UPCOMING]
            updateTitleText();

            // Price Caption đổi thành "Starting Bid"
            priceCaptionLabel.setText("Starting Bid");

            // Hiển thị khu vực đặt giá nhưng ở dạng màu nhạt hơn và không cho phép tương tác
            biddingInputArea.setVisible(true);
            biddingInputArea.setManaged(true);
            biddingInputArea.setDisable(true);
            biddingInputArea.setOpacity(0.5);

            winnerBox.setVisible(false);
            winnerBox.setManaged(false);

        } else if (status == Auction.Status.ENDED || status == Auction.Status.CANCELLED) {
            Label endedOverlay = new Label("ENDED");
            endedOverlay.setId("ended-overlay-label");
            endedOverlay.setStyle("-fx-background-color: transparent; " +
                                  "-fx-text-fill: white; " +
                                  "-fx-font-family: 'Montserrat'; " +
                                  "-fx-font-weight: 800; " +
                                  "-fx-font-size: 26px; " +
                                  "-fx-padding: 12 30 12 30; " +
                                  "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 0);");
            imageContainer.getChildren().add(endedOverlay);

            // Update title text to include [ENDED]
            updateTitleText();

            // Current bid sửa thành giá cuối cùng (Final Price)
            priceCaptionLabel.setText("Final Price");

            // Ẩn mục đặt giá và hiển thị Winner
            biddingInputArea.setVisible(false);
            biddingInputArea.setManaged(false);
            biddingInputArea.setDisable(false);
            biddingInputArea.setOpacity(1.0);

            winnerBox.setVisible(true);
            winnerBox.setManaged(true);

            int wId = viewModel.getWinnerId();
            if (wId > 0) {
                winnerLabel.setText("Selected by Bidder #" + wId);
            } else {
                // If there are bids in history, select the highest bidder (last bid)
                if (!viewModel.bids().isEmpty()) {
                    int highestBidder = viewModel.bids().get(0).getBidderId();
                    winnerLabel.setText("Selected by Bidder #" + highestBidder);
                } else {
                    winnerLabel.setText("No bids placed");
                }
            }
        } else {
            // Restore active state
            updateTitleText();

            priceCaptionLabel.setText("Current Bid");

            biddingInputArea.setVisible(true);
            biddingInputArea.setManaged(true);
            biddingInputArea.setDisable(false);
            biddingInputArea.setOpacity(1.0);

            winnerBox.setVisible(false);
            winnerBox.setManaged(false);
        }
    }

    @Override
    public void onUnload() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        if (!networkReady) {
            return;
        }
        NetworkService.getInstance().getClient().removeResponseHandler(EventType.GET_BIDS_BY_AUCTION_ID, HANDLER_ID);
        NetworkService.getInstance().getClient().removeResponseHandler(EventType.PLACE_BID, HANDLER_ID);
    }
}

