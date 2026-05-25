package com.auction.client.page.productdetail;

import com.auction.client.config.SceneRegistry;
import com.auction.client.network.ClientExceptionFactory;
import com.auction.client.scene.LifecycleAwareController;
import com.auction.client.scene.NavigationService;
import com.auction.client.service.ImageLoader;
import com.auction.client.service.NetworkService;
import com.auction.core.auction.Bid;
import com.auction.core.dto.bid.PlaceBid;
import com.auction.core.exception.DomainException;
import com.auction.core.exception.auction.AuctionClosedException;
import com.auction.core.exception.auction.InvalidBidException;
import com.auction.core.exception.auction.ShillBiddingForbiddenException;
import com.auction.core.exception.wallet.InsufficientBalanceException;
import com.auction.core.protocol.EventType;
import com.auction.core.utils.JsonMapper;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
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
import javafx.util.Duration;

public class ProductDetailPageController implements Initializable, LifecycleAwareController {

    private static final String HANDLER_ID = "PRODUCT_DETAIL_PAGE";
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML private LineChart<String, Number> bidHistoryChart;
    @FXML private CategoryAxis xAxisTime;
    @FXML private NumberAxis yAxisPrice;
    @FXML private Label categoryLabel;
    @FXML private Label titleLabel;
    @FXML private javafx.scene.layout.StackPane imageContainer;
    @FXML private Label imageLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label countdownLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label bidderCountLabel;
    @FXML private Label sellerNameLabel;
    @FXML private Label loginPromptLabel;
    @FXML private TextField bidAmountInput;
    @FXML private Button placeBidButton;
    @FXML private ListView<String> bidHistoryList;

    private final ProductDetailPageViewModel viewModel = new ProductDetailPageViewModel();
    private Timeline countdownTimeline;
    private boolean networkReady;

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

        if (networkReady && viewModel.getAuctionId() > 0) {
            fetchBidHistoryFromServer(viewModel.getAuctionId());
            // Đăng ký nhận broadcast cho phiên đấu giá này
            NetworkService.getInstance()
                    .sendRequest(
                            EventType.SUBSCRIBE_AUCTION,
                            Map.of("auctionId", viewModel.getAuctionId()));
        }
    }

    private void handleSocketResponse(String rawJson) {
        try {
            Map<?, ?> response = JsonMapper.fromJson(rawJson, Map.class);
            if (response == null
                    || !response.containsKey("data")
                    || !(response.get("data") instanceof List)) {
                return;
            }
            String dataJson = JsonMapper.toJson(response.get("data"));
            Bid[] bidsArray = JsonMapper.fromJson(dataJson, Bid[].class);
            if (bidsArray != null) {
                List<Bid> bids = Arrays.asList(bidsArray);
                Platform.runLater(() -> updateBidViews(bids));
            }
        } catch (Exception e) {
            System.err.println(
                    "Error processing socket response in ProductDetail: " + e.getMessage());
        }
    }

    private void handlePlaceBidResponse(String rawJson) {
        try {
            Map<?, ?> response = JsonMapper.fromJson(rawJson, Map.class);
            if (response == null) {
                return;
            }

            Object success = response.get("success");

            // Handle error response: parse errorCode and dispatch to typed UI handler
            if (!(success instanceof Boolean) || !((Boolean) success)) {
                Object errCodeObj = response.get("errorCode");
                String message =
                        response.get("message") instanceof String msg
                                ? msg
                                : "An error occurred. Please try again.";

                if (errCodeObj instanceof Number errNum) {
                    DomainException ex = ClientExceptionFactory.create(errNum.intValue(), message);
                    Platform.runLater(() -> dispatchBidError(ex));
                } else {
                    Platform.runLater(() -> showInfo("Bid Failed", message));
                }
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
            // Chỉ xóa ô nhập liệu khi gói tin là của chính mình (có correlationId)
            boolean isOwnBid = response.containsKey("correlationId");

            // Kiểm tra auctionId bằng .equals() (tránh so sánh reference của Integer với !=)
            Integer incomingAuctionId = incomingBid.getAuctionId();
            if (incomingAuctionId != null
                    && !incomingAuctionId.equals(viewModel.getAuctionId())) {
                return;
            }
            Platform.runLater(() -> mergeSingleBid(incomingBid, isOwnBid));
        } catch (Exception e) {
            System.err.println(
                    "Error processing place bid response in ProductDetail: " + e.getMessage());
        }
    }

    /**
     * Routes a typed DomainException to the appropriate UI action. Uses Java 21 Pattern Matching
     * switch inside Platform.runLater() to guarantee thread safety and eliminate MatchException
     * crashes from unhandled subtypes.
     */
    private void dispatchBidError(DomainException ex) {
        switch (ex) {
            case AuctionClosedException e -> {
                viewModel.biddingEnabledProperty().set(false);
                showInfo("Auction Closed", e.getMessage());
            }
            case InsufficientBalanceException e -> {
                showInfo(
                        "Insufficient Balance",
                        "Your balance is too low to hold the 30% deposit. Please top up.");
            }
            case ShillBiddingForbiddenException e -> {
                showInfo("Not Allowed", "You cannot bid on your own auction.");
            }
            case InvalidBidException e -> {
                showInfo("Invalid Bid", e.getMessage());
            }
            default -> showInfo("Bid Failed", ex.getMessage());
        }
    }

    private void fetchBidHistoryFromServer(int auctionId) {
        Map<String, Integer> payload =
                Map.of("auctionId", auctionId, "userId", viewModel.getBidderId());
        NetworkService.getInstance().sendRequest(EventType.GET_BIDS_BY_AUCTION_ID, payload);
    }

    public void drawChart(List<Bid> bids) {
        bidHistoryChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Lịch sử giá");

        List<Bid> sorted =
                bids == null
                        ? List.of()
                        : bids.stream()
                                .sorted(
                                        Comparator.comparing(
                                                Bid::getCreatedAt,
                                                Comparator.nullsLast(Comparator.naturalOrder())))
                                .toList();

        for (Bid bid : sorted) {
            String timeLabel =
                    bid.getCreatedAt() != null ? bid.getCreatedAt().format(TIME_FORMAT) : "Unknown";
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
            showInfo(
                    "Network unavailable",
                    "Cannot place bid because server connection is unavailable.");
            return;
        }

        PlaceBid payload = new PlaceBid(viewModel.getAuctionId(), viewModel.getBidderId(), amount);
        NetworkService.getInstance().sendRequest(EventType.PLACE_BID, payload);
    }

    @FXML
    private void handleSetAutoBid() {
        showInfo(
                "Auto Bid",
                "Auto Bid popup will be implemented next. Local state strategy is ready.");
    }

    private void setupBindings() {
        categoryLabel.textProperty().bind(viewModel.categoryProperty());
        titleLabel.textProperty().bind(viewModel.titleProperty());
        descriptionLabel.textProperty().bind(viewModel.descriptionProperty());

        ImageLoader.loadImage(viewModel.imageUrl(), imageContainer, imageLabel, 800);

        countdownLabel.textProperty().bind(viewModel.countdownTextProperty());
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
            NetworkService.getInstance()
                    .getClient()
                    .addResponseHandler(
                            EventType.GET_BIDS_BY_AUCTION_ID,
                            HANDLER_ID,
                            this::handleSocketResponse);
            NetworkService.getInstance()
                    .getClient()
                    .addResponseHandler(
                            EventType.PLACE_BID, HANDLER_ID, this::handlePlaceBidResponse);
            networkReady = true;
        } catch (IllegalStateException ex) {
            networkReady = false;
        }
    }

    private void startCountdownTicker() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        countdownTimeline =
                new Timeline(
                        new KeyFrame(
                                Duration.seconds(1),
                                event -> {
                                    viewModel.updateCountdown(LocalDateTime.now());
                                    if (!viewModel.isBiddingEnabled()
                                            && countdownTimeline != null) {
                                        countdownTimeline.stop();
                                    }
                                }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateBidViews(List<Bid> bids) {
        viewModel.setBids(bids);
        drawChart(viewModel.bids());
        refreshBidHistoryList(viewModel.bids());
    }

    private void mergeSingleBid(Bid bid, boolean isOwnBid) {
        List<Bid> merged = new ArrayList<>(viewModel.bids());
        merged.add(bid);
        updateBidViews(merged);
        // Chỉ xóa ô nhập liệu khi gói tin là của chính mình để không làm gián đoạn
        // người dùng khác đang gõ số tiền
        if (isOwnBid) {
            bidAmountInput.clear();
        }
    }

    private void refreshBidHistoryList(List<Bid> bids) {
        List<String> rows =
                bids.stream()
                        .map(
                                bid -> {
                                    String time =
                                            bid.getCreatedAt() == null
                                                    ? "Unknown"
                                                    : bid.getCreatedAt().format(TIME_FORMAT);
                                    String bidder =
                                            bid.getBidderId() == null
                                                    ? "#-"
                                                    : "#" + bid.getBidderId();
                                    String amount =
                                            "$"
                                                    + MONEY_FORMAT.format(
                                                            bid.getAmount() == null
                                                                    ? 0.0
                                                                    : bid.getAmount());
                                    return time + "  |  Bidder " + bidder + "  |  " + amount;
                                })
                        .toList();
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
        // Hủy đăng ký broadcast room khi rời trang (tránh nhận data rác)
        if (viewModel.getAuctionId() > 0) {
            NetworkService.getInstance()
                    .sendRequest(
                            EventType.UNSUBSCRIBE_AUCTION,
                            Map.of("auctionId", viewModel.getAuctionId()));
        }
        NetworkService.getInstance()
                .getClient()
                .removeResponseHandler(EventType.GET_BIDS_BY_AUCTION_ID, HANDLER_ID);
        NetworkService.getInstance()
                .getClient()
                .removeResponseHandler(EventType.PLACE_BID, HANDLER_ID);
    }
}
