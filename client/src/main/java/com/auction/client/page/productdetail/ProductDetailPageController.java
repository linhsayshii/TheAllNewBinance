package com.auction.client.page.productdetail;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.auction.client.config.SceneRegistry;
import com.auction.client.scene.LifecycleAwareController;
import com.auction.client.scene.NavigationService;
import com.auction.client.service.NetworkService;
import com.auction.core.auction.Bid;
import com.auction.core.protocol.EventType;
import com.auction.core.utils.JsonMapper;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

public class ProductDetailPageController implements Initializable, LifecycleAwareController {

    private static final String HANDLER_ID = "PRODUCT_DETAIL_PAGE";

    @FXML private LineChart<String, Number> bidHistoryChart;
    @FXML private CategoryAxis xAxisTime;
    @FXML private NumberAxis yAxisPrice;

    @FXML
    private void handleGoToGeneral() {
        NavigationService.getInstance().navigateTo(SceneRegistry.GENERAL_PAGE);
    }

    @FXML
    private void handleGoToLogin() {
        NavigationService.getInstance().openPopup(SceneRegistry.LOGIN_PAGE);
    }

    @FXML
    private void handleGoToRegister() {
        NavigationService.getInstance().openPopup(SceneRegistry.REGISTER_PAGE);
    }

    @FXML
    private void handleGoToProfile() {
        NavigationService.getInstance().navigateTo(SceneRegistry.PROFILE_PAGE);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bidHistoryChart.setCreateSymbols(true);
        bidHistoryChart.getStyleClass().add("bid-history-chart");

        NetworkService.getInstance().getClient()
            .addResponseHandler(EventType.GET_BIDS_BY_AUCTION_ID, HANDLER_ID, this::handleSocketResponse);

        fetchBidHistoryFromServer(1); // TODO: receive auctionId as parameter
    }

    private void handleSocketResponse(String rawJson) {
        try {
            Map<?, ?> response = JsonMapper.fromJson(rawJson, Map.class);
            if (response == null || !response.containsKey("data") || !(response.get("data") instanceof List)) {
                return;
            }
            String dataJson = JsonMapper.toJson(response.get("data"));
            Bid[] bidsArray = JsonMapper.fromJson(dataJson, Bid[].class);
            if (bidsArray != null) {
                List<Bid> bids = Arrays.asList(bidsArray);
                Platform.runLater(() -> drawChart(bids));
            }
        } catch (Exception e) {
            System.err.println("Error processing socket response in ProductDetail: " + e.getMessage());
        }
    }

    private void fetchBidHistoryFromServer(int auctionId) {
        Map<String, Integer> payload = Map.of("auctionId", auctionId);
        NetworkService.getInstance().sendRequest(EventType.GET_BIDS_BY_AUCTION_ID, payload);
    }

    public void drawChart(List<Bid> bids) {
        bidHistoryChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Lịch sử giá");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        for (Bid bid : bids) {
            String timeLabel = bid.getCreatedAt() != null
                ? bid.getCreatedAt().format(formatter) : "Unknown";
            series.getData().add(new XYChart.Data<>(timeLabel, bid.getAmount()));
        }
        bidHistoryChart.getData().add(series);
    }

    @Override
    public void onUnload() {
        NetworkService.getInstance().getClient()
            .removeResponseHandler(EventType.GET_BIDS_BY_AUCTION_ID, HANDLER_ID);
    }
}