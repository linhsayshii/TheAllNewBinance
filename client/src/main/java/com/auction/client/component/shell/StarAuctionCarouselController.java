package com.auction.client.component.shell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.auction.client.component.item.StarAuctionCardController;
import com.auction.client.service.NetworkService;
import com.auction.core.dto.auction.GetFeaturedAuctionsRequest;
import com.auction.core.dto.auction.PublicAuctionDto;
import com.auction.core.protocol.EventType;
import com.auction.core.utils.JsonMapper;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/**
 * Controller cho star-auction-carousel.fxml.
 *
 * Flow:
 *  1. initialize() → gửi GET_FEATURED_AUCTIONS tới mock/server
 *  2. Nhận response → build list StarAuctionCard và thêm vào carouselTrack
 *  3. Mỗi card chiếm đúng 100% width của viewport (prefWidth = viewport.width)
 *  4. Slide bằng TranslateTransition trên carouselTrack
 *  5. Dot indicators cập nhật theo slide hiện tại
 */
public class StarAuctionCarouselController {

    private static final Duration SLIDE_DURATION   = Duration.millis(450);
    private static final String   CARD_FXML_PATH   = "/fxml/components/item/star-auction-card.fxml";

    @FXML private Pane carouselViewport;
    @FXML private HBox carouselTrack;
    @FXML private HBox dotContainer;

    private final List<PublicAuctionDto> items    = new ArrayList<>();
    private final List<Region>           dots     = new ArrayList<>();
    private final List<StarAuctionCardController> controllers = new ArrayList<>();
    private int currentIndex = 0;
    private boolean animating = false;
    private Timeline autoScrollTimeline;

    @FXML
    private void initialize() {
        // Hide dot navigation container completely
        dotContainer.setVisible(false);
        dotContainer.setManaged(false);

        // Bind card width to viewport width once laid out
        carouselViewport.widthProperty().addListener((obs, oldW, newW) -> {
            if (newW.doubleValue() > 0) {
                resizeCards(newW.doubleValue());
            }
        });

        // Clip viewport to prevent overflow bleed
        // Using fixed height 320 from CSS to prevent vertical clipping issues on navigation
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setHeight(320); 
        carouselViewport.setClip(clip);
        
        carouselViewport.widthProperty().addListener((obs, oldW, newW) -> {
            clip.setWidth(newW.doubleValue());
        });

        carouselTrack.setManaged(false);
        carouselTrack.setLayoutY(0);
        carouselTrack.setPrefHeight(320);
        carouselTrack.setMinHeight(320);
        carouselTrack.setMaxHeight(320);
        carouselTrack.autosize();

        fetchFeaturedAuctions();
        registerRealTimeHandlers();
    }

    private void registerRealTimeHandlers() {
        try {
            NetworkService.getInstance().getClient()
                .addResponseHandler(EventType.PLACE_BID, "CAROUSEL_PLACE_BID", this::handleCarouselPlaceBid);
        } catch (Exception e) {
            System.err.println("[StarCarousel] Network client not ready, real-time pricing disabled: " + e.getMessage());
        }
    }

    private void handleCarouselPlaceBid(String rawJson) {
        try {
            java.util.Map<?, ?> response = JsonMapper.fromJson(rawJson, java.util.Map.class);
            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                return;
            }
            Object data = response.get("data");
            if (data == null) {
                return;
            }
            com.auction.core.auction.Bid incomingBid = JsonMapper.fromJson(JsonMapper.toJson(data), com.auction.core.auction.Bid.class);
            if (incomingBid != null && incomingBid.getAuctionId() != null) {
                int bidAuctionId = incomingBid.getAuctionId();
                double amount = incomingBid.getAmount();
                Platform.runLater(() -> {
                    for (StarAuctionCardController ctrl : controllers) {
                        if (ctrl.getAuctionId() != null && ctrl.getAuctionId() == bidAuctionId) {
                            ctrl.updatePrice(amount);
                        }
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("[StarCarousel] Error handling PLACE_BID: " + e.getMessage());
        }
    }

    // ── Data Loading ────────────────────────────────────────────────────

    private void fetchFeaturedAuctions() {
        String correlationId = NetworkService.getInstance()
            .sendRequest(EventType.GET_FEATURED_AUCTIONS, new GetFeaturedAuctionsRequest());

        NetworkService.getInstance().addCorrelationHandler(correlationId, rawJson -> {
            Platform.runLater(() -> handleResponse(rawJson));
        });
    }

    @SuppressWarnings("unchecked")
    private void handleResponse(String rawJson) {
        try {
            java.util.Map<?, ?> root = JsonMapper.fromJson(rawJson, java.util.Map.class);
            if (!Boolean.TRUE.equals(root.get("success"))) {
                System.err.println("[StarCarousel] Server returned error: " + root.get("message"));
                return;
            }

            Object dataRaw = root.get("data");
            if (!(dataRaw instanceof List<?> dataList) || dataList.isEmpty()) {
                System.out.println("[StarCarousel] No featured auctions — carousel hidden.");
                hideCarousel();
                return;
            }

            // Deserialise each entry into PublicAuctionDto
            for (Object entry : dataList) {
                try {
                    String entryJson = JsonMapper.toJson(entry);
                    PublicAuctionDto dto = JsonMapper.fromJson(entryJson, PublicAuctionDto.class);
                    if (dto != null) items.add(dto);
                } catch (Exception e) {
                    System.err.println("[StarCarousel] Failed to parse DTO: " + e.getMessage());
                }
            }

            if (items.isEmpty()) {
                hideCarousel();
                return;
            }

            buildCarousel();

        } catch (Exception e) {
            System.err.println("[StarCarousel] Error parsing response: " + e.getMessage());
            hideCarousel();
        }
    }

    // ── Build UI ─────────────────────────────────────────────────────────

    private void buildCarousel() {
        carouselTrack.getChildren().clear();
        dotContainer.getChildren().clear();
        dots.clear();
        controllers.clear();
        currentIndex = 0;

        // 1. Add all real items to the track
        for (int i = 0; i < items.size(); i++) {
            addCardToTrack(items.get(i));
        }

        // 2. Support Seamless Infinite Loop: append a clone of the first card at the end of the track
        if (items.size() > 1) {
            addCardToTrack(items.get(0));
        }

        // Position track at index 0 (no translation)
        carouselTrack.setTranslateX(0);
        carouselTrack.setLayoutX(0);
        carouselTrack.setLayoutY(0);
        carouselTrack.requestLayout();

        // 3. Start Auto-Scroll if we have more than 1 item
        if (items.size() > 1) {
            startAutoScroll();
        }
    }

    private void addCardToTrack(PublicAuctionDto dto) {
        double viewportWidth = carouselViewport.getWidth();
        if (viewportWidth <= 0) viewportWidth = 900;
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource(CARD_FXML_PATH)
            );
            Region card = loader.load();
            StarAuctionCardController ctrl = loader.getController();
            ctrl.setData(dto);
            controllers.add(ctrl);

            // Force each card to exactly fill the viewport width
            card.setPrefWidth(viewportWidth);
            card.setMinWidth(viewportWidth);
            card.setMaxWidth(viewportWidth);

            carouselTrack.getChildren().add(card);
        } catch (IOException e) {
            System.err.println("[StarCarousel] Could not load card FXML: " + e.getMessage());
        }
    }

    // ── Auto-scroll & Play logic ─────────────────────────────────────────

    private void startAutoScroll() {
        if (autoScrollTimeline != null) {
            autoScrollTimeline.stop();
        }
        autoScrollTimeline = new Timeline(new KeyFrame(Duration.seconds(10), ev -> {
            if (!items.isEmpty()) {
                int targetIndex = currentIndex + 1;
                slideTo(targetIndex);
            }
        }));
        autoScrollTimeline.setCycleCount(Timeline.INDEFINITE);
        autoScrollTimeline.play();

        // Stop auto-scroll if carousel is detached from scene
        carouselViewport.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && autoScrollTimeline != null) {
                autoScrollTimeline.stop();
            }
        });
    }

    private void resetAutoScroll() {
        if (autoScrollTimeline != null) {
            autoScrollTimeline.playFromStart();
        }
    }

    // ── Navigation ───────────────────────────────────────────────────────

    @FXML
    private void handlePrev(MouseEvent event) {
        if (animating || items.isEmpty()) return;
        resetAutoScroll();
        int targetIndex = currentIndex - 1;
        slideTo(targetIndex);
    }

    @FXML
    private void handleNext(MouseEvent event) {
        if (animating || items.isEmpty()) return;
        resetAutoScroll();
        int targetIndex = currentIndex + 1;
        slideTo(targetIndex);
    }

    // ── Slide Logic ──────────────────────────────────────────────────────

    /**
     * Slides the carousel track to reveal card at {@code targetIndex}.
     * Supports seamless cloning-based infinite looping.
     */
    private void slideTo(int targetIndex) {
        if (animating || items.isEmpty()) return;
        
        int N = items.size();
        if (N <= 1) {
            animating = false;
            return;
        }

        animating = true;

        double cardWidth = carouselViewport.getWidth();
        if (cardWidth <= 0 && !carouselTrack.getChildren().isEmpty()) {
            cardWidth = ((Region) carouselTrack.getChildren().get(0)).getWidth();
        }

        if (targetIndex < 0) {
            // Seamless loop transition from first (0) to last (N-1) by sliding backwards:
            // 1. Instantly snap to the clone card at index N (showing A') without animation
            carouselTrack.setTranslateX(-N * cardWidth);
            // 2. Animate backwards to index N-1 (showing C)
            currentIndex = N;
            targetIndex = N - 1;
        }

        double targetX = -targetIndex * cardWidth;

        TranslateTransition tt = new TranslateTransition(SLIDE_DURATION, carouselTrack);
        tt.setToX(targetX);
        tt.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
        final int finalTarget = targetIndex;
        tt.setOnFinished(ev -> {
            currentIndex = finalTarget;
            // If we scrolled forward to the clone card at index N, instantly snap back to real card at 0
            if (currentIndex == N) {
                carouselTrack.setTranslateX(0);
                currentIndex = 0;
            }
            animating = false;
        });
        tt.play();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void resizeCards(double newWidth) {
        for (javafx.scene.Node node : carouselTrack.getChildren()) {
            if (node instanceof Region r) {
                r.setPrefWidth(newWidth);
                r.setMinWidth(newWidth);
                r.setMaxWidth(newWidth);
            }
        }
        // Re-snap translation to current index after resize
        carouselTrack.setTranslateX(-currentIndex * newWidth);
        carouselTrack.autosize();
        carouselTrack.layout();
    }

    private void hideCarousel() {
        // Hide the entire shell from layout
        if (carouselViewport != null && carouselViewport.getScene() != null) {
            carouselViewport.getParent().getParent().setVisible(false);
            carouselViewport.getParent().getParent().setManaged(false);
        }
    }
}
