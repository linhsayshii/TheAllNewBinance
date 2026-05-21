package com.auction.client.page.profile;

import com.auction.client.component.item.ProfileAuctionCardController;
import com.auction.client.component.modal.PromoteModalController;
import com.auction.client.component.profile.ProfileSidebarController;
import com.auction.client.config.SceneRegistry;
import com.auction.client.dto.ProfileAuctionCardUiModel;
import com.auction.client.scene.DataReceivable;
import com.auction.client.scene.LifecycleAwareController;
import com.auction.client.scene.NavigationService;
import com.auction.client.service.UserSessionService;
import java.io.IOException;
import java.util.List;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the Personal Profile page (personal-profile-page.fxml).
 *
 * <p>This page is for the currently logged-in user only: — Dashboard: balance summary + active
 * bid/listing counts — Finance: wallet details, deposit/withdraw — My Bids: active bids + won
 * auctions — My Listings: live / pending / sold / unsold tabs
 *
 * <p>Navigation here always starts from the header's "Profile" link, which passes {@code {userId:
 * <current user id>}} via DataReceivable.
 */
public class PersonalProfileController implements DataReceivable, LifecycleAwareController {

    private static final String PROFILE_CARD_FXML =
            "/fxml/components/item/profile-auction-card.fxml";
    private static final String PROMOTE_MODAL_FXML = "/fxml/components/modal/promote-modal.fxml";

    // ---- Sidebar (injected via fx:include fx:id="profileSidebar") ----
    @FXML private ProfileSidebarController profileSidebarController;

    // ---- Nav toggle buttons ----
    @FXML private ToggleGroup navGroup;
    @FXML private ToggleButton navDashboard;
    @FXML private ToggleButton navFinance;
    @FXML private ToggleButton navMyBids;
    @FXML private ToggleButton navMyListings;

    // ---- Content StackPane ----
    @FXML private StackPane contentArea;
    @FXML private StackPane modalHost;

    // ---- Dashboard section ----
    @FXML private VBox dashboardSection;
    @FXML private Label dashTotalBalanceLabel;
    @FXML private Label dashAvailableBalanceLabel;
    @FXML private Label dashActiveBidsLabel;
    @FXML private Label dashActiveListingsLabel;

    // ---- Finance section ----
    @FXML private VBox financeSection;
    @FXML private Label finTotalBalanceLabel;
    @FXML private Label finLockedBalanceLabel;
    @FXML private Label finAvailableBalanceLabel;

    // ---- My Bids section ----
    @FXML private VBox myBidsSection;
    @FXML private ToggleGroup bidsTabGroup;
    @FXML private ToggleButton bidsTabActive;
    @FXML private ToggleButton bidsTabWon;
    @FXML private FlowPane bidsCardsContainer;
    @FXML private Label bidsEmptyLabel;

    // ---- My Listings section ----
    @FXML private VBox myListingsSection;
    @FXML private Label listingsStatsLabel;
    @FXML private ToggleGroup listingsTabGroup;
    @FXML private ToggleButton listingsTabLive;
    @FXML private ToggleButton listingsTabPending;
    @FXML private ToggleButton listingsTabSold;
    @FXML private ToggleButton listingsTabUnsold;
    @FXML private FlowPane listingsCardsContainer;
    @FXML private Label listingsEmptyLabel;

    private final ProfilePageViewModel viewModel = new ProfilePageViewModel();
    private boolean dataLoaded = false;

    // ------------------------------------------------------------------ //
    //  FXML Lifecycle                                                      //
    // ------------------------------------------------------------------ //

    @FXML
    private void initialize() {
        if (UserSessionService.getInstance().isAuthenticated()) {
            viewModel.loadFromSession();
            profileSidebarController.bind(viewModel);
            bindDashboard();
            bindFinance();
        }
        setupNavigation();
    }

    private void setupNavigation() {
        navGroup.selectedToggleProperty()
                .addListener(
                        (obs, oldToggle, newToggle) -> {
                            if (newToggle == null) {
                                oldToggle.setSelected(true);
                                return;
                            }
                            showSection(newToggle);
                        });
        navDashboard.setSelected(true);
        showSection(navDashboard);
    }

    private void showSection(Toggle selected) {
        List<Node> all =
                List.of(dashboardSection, financeSection, myBidsSection, myListingsSection);
        for (Node n : all) {
            n.setVisible(false);
            n.setManaged(false);
        }

        if (selected == navDashboard) {
            show(dashboardSection);
        } else if (selected == navFinance) {
            show(financeSection);
        } else if (selected == navMyBids) {
            show(myBidsSection);
            if (!dataLoaded) {
                loadDataAsync();
            } else {
                renderBidsTab(
                        bidsTabActive.isSelected()
                                ? viewModel.getActiveBids()
                                : viewModel.getWonAuctions());
            }
        } else if (selected == navMyListings) {
            show(myListingsSection);
            if (!dataLoaded) {
                loadDataAsync();
            } else {
                refreshListingsStats();
                renderActiveListingsTab();
            }
        }
    }

    private void show(Node n) {
        n.setVisible(true);
        n.setManaged(true);
    }

    // ------------------------------------------------------------------ //
    //  DataReceivable                                                      //
    // ------------------------------------------------------------------ //

    @Override
    public void onDataReceived(java.util.Map<String, Object> data) {
        if (data == null) {
            return;
        }
        // Reload fresh data every time this page is navigated to
        viewModel.loadFromSession();
        profileSidebarController.bind(viewModel);
        bindDashboard();
        bindFinance();
        dataLoaded = false;
        loadDataAsync();
        navDashboard.setSelected(true);
        showSection(navDashboard);
    }

    // ------------------------------------------------------------------ //
    //  Binding                                                             //
    // ------------------------------------------------------------------ //

    private void bindDashboard() {
        dashTotalBalanceLabel.setText(viewModel.getFormattedTotalBalance());
        dashAvailableBalanceLabel.setText(viewModel.getFormattedAvailableBalance());
        viewModel
                .activeBidsCountProperty()
                .addListener(
                        (obs, o, n) ->
                                Platform.runLater(() -> dashActiveBidsLabel.setText(n.toString())));
        viewModel
                .activeListingsCountProperty()
                .addListener(
                        (obs, o, n) ->
                                Platform.runLater(
                                        () -> dashActiveListingsLabel.setText(n.toString())));
    }

    private void bindFinance() {
        finTotalBalanceLabel.setText(viewModel.getFormattedTotalBalance());
        finLockedBalanceLabel.setText(viewModel.getFormattedLockedBalance());
        finAvailableBalanceLabel.setText(viewModel.getFormattedAvailableBalance());
    }

    // ------------------------------------------------------------------ //
    //  Async loading                                                       //
    // ------------------------------------------------------------------ //

    private void loadDataAsync() {
        dataLoaded = true;
        java.util.concurrent.CompletableFuture.runAsync(
                () -> {
                    viewModel.fetchMyBids();
                    viewModel.fetchMyListings();

                    Platform.runLater(
                            () -> {
                                dashActiveBidsLabel.setText(
                                        String.valueOf(viewModel.activeBidsCountProperty().get()));
                                dashActiveListingsLabel.setText(
                                        String.valueOf(
                                                viewModel.activeListingsCountProperty().get()));

                                Toggle current = navGroup.getSelectedToggle();
                                if (current == navMyBids) {
                                    renderBidsTab(
                                            bidsTabActive.isSelected()
                                                    ? viewModel.getActiveBids()
                                                    : viewModel.getWonAuctions());
                                } else if (current == navMyListings) {
                                    refreshListingsStats();
                                    renderActiveListingsTab();
                                }
                            });
                });
    }

    // ------------------------------------------------------------------ //
    //  My Bids sub-tabs                                                    //
    // ------------------------------------------------------------------ //

    @FXML
    private void handleBidsTabActive() {
        renderBidsTab(viewModel.getActiveBids());
    }

    @FXML
    private void handleBidsTabWon() {
        renderBidsTab(viewModel.getWonAuctions());
    }

    private void renderBidsTab(List<ProfileAuctionCardUiModel> cards) {
        bidsCardsContainer.getChildren().clear();
        boolean empty = cards.isEmpty();
        bidsEmptyLabel.setVisible(empty);
        bidsEmptyLabel.setManaged(empty);
        if (!empty) {
            cards.forEach(c -> bidsCardsContainer.getChildren().add(loadCard(c)));
        }
    }

    // ------------------------------------------------------------------ //
    //  My Listings sub-tabs                                                //
    // ------------------------------------------------------------------ //

    @FXML
    private void handleListingsTabLive() {
        renderListingsTab(viewModel.getLiveListings());
    }

    @FXML
    private void handleListingsTabPending() {
        renderListingsTab(viewModel.getPendingListings());
    }

    @FXML
    private void handleListingsTabSold() {
        renderListingsTab(viewModel.getSoldListings());
    }

    @FXML
    private void handleListingsTabUnsold() {
        renderListingsTab(viewModel.getUnsoldListings());
    }

    private void renderActiveListingsTab() {
        listingsTabLive.setSelected(true);
        renderListingsTab(viewModel.getLiveListings());
    }

    private void renderListingsTab(List<ProfileAuctionCardUiModel> cards) {
        listingsCardsContainer.getChildren().clear();
        boolean empty = cards.isEmpty();
        listingsEmptyLabel.setVisible(empty);
        listingsEmptyLabel.setManaged(empty);
        if (!empty) {
            // Determine if we're on Live or Pending tab (the tabs that allow Promote)
            boolean isLiveOrPending =
                    listingsTabLive.isSelected() || listingsTabPending.isSelected();
            cards.forEach(
                    c -> {
                        Runnable onPromote =
                                isLiveOrPending
                                        ? () -> openPromoteModal(c.auctionId(), c.title())
                                        : null;
                        listingsCardsContainer.getChildren().add(loadCard(c, onPromote));
                    });
        }
    }

    private void refreshListingsStats() {
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
        return loadCard(model, null);
    }

    private Node loadCard(ProfileAuctionCardUiModel model, Runnable onPromote) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(PROFILE_CARD_FXML));
            Node root = loader.load();
            ProfileAuctionCardController ctrl = loader.getController();
            ctrl.setData(model, onPromote);
            return root;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load profile-auction-card component", e);
        }
    }

    // ── Promote Modal ────────────────────────────────────────────────────

    private void openPromoteModal(Integer auctionId, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(PROMOTE_MODAL_FXML));
            javafx.scene.Node root = loader.load();
            PromoteModalController modal = loader.getController();
            modal.open(
                    modalHost,
                    root,
                    auctionId,
                    title,
                    () -> {
                        // Refresh My Listings after successful promote
                        dataLoaded = false;
                        loadDataAsync();
                    });
        } catch (IOException e) {
            System.err.println("[Profile] Failed to load promote-modal: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //
    //  FXML action stubs (nav handled by ToggleGroup listener)            //
    // ------------------------------------------------------------------ //

    @FXML
    private void handleNavDashboard() {
        /* handled by ToggleGroup listener */
    }

    @FXML
    private void handleNavFinance() {
        /* handled by ToggleGroup listener */
    }

    @FXML
    private void handleNavMyBids() {
        /* handled by ToggleGroup listener */
    }

    @FXML
    private void handleNavMyListings() {
        /* handled by ToggleGroup listener */
    }

    @FXML
    private void handleCreateListing() {
        NavigationService.getInstance().navigateTo(SceneRegistry.CREATE_LISTING_PAGE);
    }

    @FXML
    private void handleDeposit() {
        showComingSoon("Deposit");
    }

    @FXML
    private void handleWithdraw() {
        showComingSoon("Withdraw");
    }

    private void showComingSoon(String feature) {
        new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.INFORMATION,
                        feature + " functionality is coming soon!")
                .show();
    }

    // ------------------------------------------------------------------ //
    //  LifecycleAwareController                                            //
    // ------------------------------------------------------------------ //

    @Override
    public void onUnload() {
        /* no persistent resources to clean up */
    }
}
