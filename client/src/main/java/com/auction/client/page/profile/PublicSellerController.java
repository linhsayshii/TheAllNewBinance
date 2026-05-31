package com.auction.client.page.profile;

import com.auction.client.component.item.ProfileAuctionCardController;
import com.auction.client.component.profile.ProfileSidebarController;
import com.auction.client.dto.ProfileAuctionCardUiModel;
import com.auction.client.scene.DataReceivable;
import com.auction.client.scene.LifecycleAwareController;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the Public Seller Profile page (public-seller-page.fxml).
 *
 * <p>Receives {@code {sellerId: <int>}} via DataReceivable when the user clicks a seller's name on
 * the Auction Detail page (or Sellers page).
 *
 * <p>Shows only the seller's public Live and Sold listings. No Dashboard, Finance, or My Bids
 * sections.
 */
public class PublicSellerController implements DataReceivable, LifecycleAwareController {

    private static final String PROFILE_CARD_FXML =
            "/fxml/components/item/profile-auction-card.fxml";

    // ---- Sidebar (injected via fx:include fx:id="profileSidebar") ----
    @FXML private ProfileSidebarController profileSidebarController;

    // ---- Nav ----
    @FXML private ToggleGroup navGroup;
    @FXML private ToggleButton navListings;

    // ---- Listings section ----
    @FXML private VBox listingsSection;
    @FXML private Label listingsStatsLabel;
    @FXML private ToggleGroup listingsTabGroup;
    @FXML private ToggleButton listingsTabLive;
    @FXML private ToggleButton listingsTabSold;
    @FXML private FlowPane listingsCardsContainer;
    @FXML private Label listingsEmptyLabel;

    private final ProfilePageViewModel viewModel = new ProfilePageViewModel();

    // ------------------------------------------------------------------ //
    //  DataReceivable                                                      //
    // ------------------------------------------------------------------ //

    @Override
    public void onDataReceived(Map<String, Object> data) {
        if (data == null) {
            return;
        }

        if (data.containsKey("sellerId")) {
            int sellerId = ((Number) data.get("sellerId")).intValue();
            String sellerName = (String) data.getOrDefault("sellerName", "Seller #" + sellerId);
            String email = (String) data.getOrDefault("email", "");
            String joinDate = (String) data.getOrDefault("joinDate", "");

            viewModel.loadPublicSellerView(sellerId, sellerName, email, joinDate);
            profileSidebarController.bind(viewModel);
            loadListingsAsync(sellerId);
        }
    }

    // ------------------------------------------------------------------ //
    //  Async loading                                                       //
    // ------------------------------------------------------------------ //

    private void loadListingsAsync(int sellerId) {
        java.util.concurrent.CompletableFuture.runAsync(
                () -> {
                    viewModel.fetchMyListings(); // uses targetUserId = sellerId internally
                    Platform.runLater(
                            () -> {
                                refreshStats();
                                listingsTabLive.setSelected(true);
                                renderTab(viewModel.getLiveListings());
                            });
                });
    }

    // ------------------------------------------------------------------ //
    //  Sub-tab handlers                                                    //
    // ------------------------------------------------------------------ //

    @FXML
    private void handleNavListings() {
        /* single nav item — always visible */
    }

    @FXML
    private void handleListingsTabLive() {
        renderTab(viewModel.getLiveListings());
    }

    @FXML
    private void handleListingsTabSold() {
        renderTab(viewModel.getSoldListings());
    }

    private void renderTab(List<ProfileAuctionCardUiModel> cards) {
        listingsCardsContainer.getChildren().clear();
        boolean empty = cards.isEmpty();
        listingsEmptyLabel.setVisible(empty);
        listingsEmptyLabel.setManaged(empty);
        if (!empty) {
            cards.forEach(c -> listingsCardsContainer.getChildren().add(loadCard(c)));
        }
    }

    private void refreshStats() {
        int total = viewModel.totalListingsCountProperty().get();
        int sold = viewModel.soldListingsCountProperty().get();
        listingsStatsLabel.setText(
                total
                        + " listings · "
                        + sold
                        + " sold · "
                        + viewModel.getSellerSuccessRate()
                        + " success rate");
    }

    // ------------------------------------------------------------------ //
    //  Card loader                                                         //
    // ------------------------------------------------------------------ //

    private Node loadCard(ProfileAuctionCardUiModel model) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(PROFILE_CARD_FXML));
            Node root = loader.load();
            ProfileAuctionCardController ctrl = loader.getController();
            ctrl.setData(model);
            return root;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load profile-auction-card component", e);
        }
    }

    // ------------------------------------------------------------------ //
    //  LifecycleAwareController                                            //
    // ------------------------------------------------------------------ //

    @Override
    public void onUnload() {
        /* nothing to clean up */
    }
}
