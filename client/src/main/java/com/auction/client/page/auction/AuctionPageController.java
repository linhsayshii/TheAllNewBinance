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
    @FXML private Label imagePlaceholderLabel;
    @FXML private Label descriptionLabel;
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
            Auction auction = JsonMapper.fromJson(dataJson, Auction.class);
            if (auction != null) {
                Platform.runLater(() -> {
                    viewModel.applyAuctionData(auction, null, null, null, null);
                    // Update title with auction info
                    if (auction.getItemId() != null) {
                        viewModel.titleProperty().set("Auction #" + auction.getId());
                    }
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
        Map<String, Integer> payload = Map.of(
            "auctionId", auctionId,
            "userId", viewModel.getBidderId()
        );
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

        PlaceBid payload = new PlaceBid(viewModel.getAuctionId(), viewModel.getBidderId(), amount);
        NetworkService.getInstance().sendRequest(EventType.PLACE_BID, payload);
    }

    @FXML
    private void handleSetAutoBid() {
        showInfo("Auto Bid", "Auto Bid popup will be implemented next. Local state strategy is ready.");
    }

    private void setupBindings() {
        categoryLabel.textProperty().bind(viewModel.categoryProperty());
        titleLabel.textProperty().bind(viewModel.titleProperty());
        descriptionLabel.textProperty().bind(viewModel.descriptionProperty());
        imagePlaceholderLabel.textProperty().bind(viewModel.imageTextProperty());
        currentBidLabel.textProperty().bind(viewModel.currentBidDisplayProperty());
        bidderCountLabel.textProperty().bind(viewModel.bidderCountTextProperty());
        sellerNameLabel.textProperty().bind(viewModel.sellerNameProperty());
        loginPromptLabel.textProperty().bind(viewModel.loginPromptProperty());

        bidAmountInput.disableProperty().bind(viewModel.biddingEnabledProperty().not());
        placeBidButton.disableProperty().bind(viewModel.biddingEnabledProperty().not());
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
            if (!viewModel.isBiddingEnabled() && countdownTimeline != null) {
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
        NavigationService.getInstance().navigateTo(SceneRegistry.PROFILE_PAGE,
                java.util.Map.of("sellerId", viewModel.getSellerId()));
    }

    private void updateCountdownLabels(LocalDateTime now) {
        dayLabel.setText(now.format(DAY_TIME_FORMAT));
        LocalDateTime end = viewModel.endTimeProperty().get();
        if (end == null) {
            countdownDaysLabel.setText("00");
            countdownHoursLabel.setText("00");
            countdownMinutesLabel.setText("00");
            countdownSecondsLabel.setText("00");
            return;
        }
        Duration remaining = Duration.between(now, end);
        if (remaining.isNegative() || remaining.isZero()) {
            countdownDaysLabel.setText("00");
            countdownHoursLabel.setText("00");
            countdownMinutesLabel.setText("00");
            countdownSecondsLabel.setText("00");
            return;
        }
        long totalSecs = remaining.getSeconds();
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

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
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

