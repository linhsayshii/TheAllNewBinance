package com.auction.client.page.profile;

import java.io.IOException;
import java.util.List;

import com.auction.client.component.item.ProfileAuctionCardController;
import com.auction.client.component.modal.PromoteModalController;
import com.auction.client.component.profile.ProfileSidebarController;
import com.auction.client.config.SceneRegistry;
import com.auction.client.dto.ProfileAuctionCardUiModel;
import com.auction.client.scene.DataReceivable;
import com.auction.client.scene.LifecycleAwareController;
import com.auction.client.scene.NavigationService;
import com.auction.client.service.UserSessionService;
import com.auction.client.service.notification.NotificationService;
import com.auction.client.service.notification.NotificationType;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
    @FXML private TextField walletAmountField;
    @FXML private VBox transactionListContainer;
    @FXML private Label txEmptyLabel;

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
            setupDynamicBindings();
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
            loadTransactionHistoryAsync();
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
        setupDynamicBindings();
        dataLoaded = false;
        loadDataAsync();
        navDashboard.setSelected(true);
        showSection(navDashboard);
    }

    // ------------------------------------------------------------------ //
    //  Binding                                                             //
    // ------------------------------------------------------------------ //

    private void setupDynamicBindings() {
        // Ràng buộc động một chiều đồng bộ tiền tệ tự động
        dashTotalBalanceLabel.textProperty().bind(javafx.beans.binding.Bindings.format("$%,.2f", viewModel.totalBalanceProperty()));
        dashAvailableBalanceLabel.textProperty().bind(javafx.beans.binding.Bindings.format("$%,.2f", viewModel.availableBalanceProperty()));

        finTotalBalanceLabel.textProperty().bind(javafx.beans.binding.Bindings.format("$%,.2f", viewModel.totalBalanceProperty()));
        finLockedBalanceLabel.textProperty().bind(javafx.beans.binding.Bindings.format("$%,.2f", viewModel.lockedBalanceProperty()));
        finAvailableBalanceLabel.textProperty().bind(javafx.beans.binding.Bindings.format("$%,.2f", viewModel.availableBalanceProperty()));

        // Ràng buộc đếm số lượng đấu giá
        dashActiveBidsLabel.textProperty().bind(viewModel.activeBidsCountProperty().asString());
        dashActiveListingsLabel.textProperty().bind(viewModel.activeListingsCountProperty().asString());
    }

    private void loadTransactionHistoryAsync() {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            java.util.List<ProfilePageViewModel.TransactionRow> rows =
                    viewModel.fetchTransactionHistory();
            Platform.runLater(() -> renderTransactionHistory(rows));
        });
    }

    private void renderTransactionHistory(
            java.util.List<ProfilePageViewModel.TransactionRow> rows) {
        transactionListContainer.getChildren().clear();
        if (rows.isEmpty()) {
            txEmptyLabel.setVisible(true);
            txEmptyLabel.setManaged(true);
            return;
        }
        txEmptyLabel.setVisible(false);
        txEmptyLabel.setManaged(false);
        for (ProfilePageViewModel.TransactionRow row : rows) {
            transactionListContainer.getChildren().add(buildTransactionRow(row));
        }
    }

    private javafx.scene.Node buildTransactionRow(ProfilePageViewModel.TransactionRow row) {
        javafx.scene.control.Label typeLabel = new javafx.scene.control.Label(
                row.isDeposit() ? "+ DEPOSIT" : "\u2212 WITHDRAW");
        typeLabel.setStyle(row.isDeposit()
                ? "-fx-text-fill: #26de81; -fx-font-weight: bold;"
                : "-fx-text-fill: #fc5c65; -fx-font-weight: bold;");

        javafx.scene.control.Label amountLabel =
                new javafx.scene.control.Label(row.formattedAmount());
        amountLabel.setStyle("-fx-font-weight: bold;");

        javafx.scene.control.Label dateLabel = new javafx.scene.control.Label(row.date());
        dateLabel.setStyle("-fx-text-fill: #8696a7;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(12,
                typeLabel, spacer, amountLabel, dateLabel);
        hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        hbox.setStyle("-fx-padding: 8 12; -fx-background-color: #1c2a3a;"
                + " -fx-background-radius: 6;");
        return hbox;
    }

    // ------------------------------------------------------------------ //
    //  Async loading                                                       //
    // ------------------------------------------------------------------ //

    private void loadDataAsync() {
        java.util.concurrent.CompletableFuture.runAsync(
                () -> {
                    viewModel.fetchMyBids();
                    viewModel.fetchMyListings();

                    Platform.runLater(
                            () -> {
                                dataLoaded = true; // Chỉ đánh dấu loaded khi dữ liệu thực sự đã về!

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
        processWalletAction("Deposit Funds", "Deposit", true);
    }

    @FXML
    private void handleWithdraw() {
        processWalletAction("Withdraw Funds", "Withdraw", false);
    }

    private void processWalletAction(String title, String actionLabel, boolean isDeposit) {
        String input = walletAmountField.getText();
        if (input == null || input.isBlank()) {
            NotificationService.getInstance().show("Please enter an amount.", NotificationType.WARNING);
            return;
        }

        java.math.BigDecimal amount;
        try {
            amount = new java.math.BigDecimal(input.trim());
            if (amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                NotificationService.getInstance().show("Amount must be greater than 0.", NotificationType.WARNING);
                return;
            }
        } catch (NumberFormatException ex) {
            NotificationService.getInstance().show("Please enter a valid numeric amount.", NotificationType.WARNING);
            return;
        }

        com.auction.core.protocol.EventType eventType = isDeposit
                ? com.auction.core.protocol.EventType.DEPOSIT
                : com.auction.core.protocol.EventType.WITHDRAW;

        com.auction.client.service.NetworkService ns =
                com.auction.client.service.NetworkService.getInstance();

        Object payload = isDeposit
                ? new com.auction.core.dto.wallet.DepositRequest(
                        UserSessionService.getInstance().getCurrentUser().getId(), amount)
                : new com.auction.core.dto.wallet.WithdrawRequest(
                        UserSessionService.getInstance().getCurrentUser().getId(), amount);

        ns.sendRequestAsync(eventType, payload)
                .thenAccept(raw -> {
                    try {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> resp =
                                com.auction.core.utils.JsonMapper.fromJson(raw, java.util.Map.class);
                        boolean success = Boolean.TRUE.equals(resp.get("success"));
                        if (success) {
                            // Refresh balance from response data
                            Object data = resp.get("data");
                            if (data instanceof java.util.Map<?, ?> dataMap) {
                                updateBalanceFromResponse(dataMap, amount, isDeposit);
                            } else {
                                adjustLocalBalance(amount, isDeposit);
                            }
                            Platform.runLater(() -> {
                                walletAmountField.clear();
                                NotificationService.getInstance().show(
                                        actionLabel + " of $" + amount.toPlainString() + " completed successfully.",
                                        NotificationType.SUCCESS);
                                loadTransactionHistoryAsync(); // Tự động load lại lịch sử!
                            });
                        } else {
                            String msg = resp.get("message") != null
                                    ? String.valueOf(resp.get("message"))
                                    : actionLabel + " failed. Please check your balance and try again.";
                            Platform.runLater(() -> NotificationService.getInstance().show(
                                    actionLabel + " failed: " + msg, NotificationType.ERROR));
                        }
                    } catch (Exception ex) {
                        Platform.runLater(() -> NotificationService.getInstance().show(
                                "Failed to process response: " + ex.getMessage(), NotificationType.ERROR));
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> NotificationService.getInstance().show(
                            actionLabel + " request failed. Please try again.", NotificationType.ERROR));
                    return null;
                });
    }

    /**
     * Updates the balance in UserSessionService from the server response payload.
     * Falls back to local adjustment when the server does not echo balance fields.
     */
    private void updateBalanceFromResponse(
            java.util.Map<?, ?> data,
            java.math.BigDecimal amount,
            boolean isDeposit) {
        com.auction.core.users.User user =
                UserSessionService.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        Object balObj = data.get("balance");
        Object lockedObj = data.get("lockedBalance");
        if (balObj != null) {
            java.math.BigDecimal newBal = new java.math.BigDecimal(String.valueOf(balObj));
            java.math.BigDecimal newLocked = lockedObj != null
                    ? new java.math.BigDecimal(String.valueOf(lockedObj))
                    : user.getLockedBalance();
            user.deposit(newBal.subtract(user.getBalance()
                    .add(isDeposit ? java.math.BigDecimal.ZERO : amount)));
        } else {
            adjustLocalBalance(amount, isDeposit);
        }
        viewModel.loadFromSession();
    }

    /**
     * Applies the transaction locally to the User domain object when the server
     * response does not include the updated balance (e.g. MockMode).
     */
    private void adjustLocalBalance(java.math.BigDecimal amount, boolean isDeposit) {
        com.auction.core.users.User user =
                UserSessionService.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        if (isDeposit) {
            user.deposit(amount);
        } else {
            user.withdraw(amount);
        }
        viewModel.loadFromSession();
    }

    // ------------------------------------------------------------------ //
    //  LifecycleAwareController                                            //
    // ------------------------------------------------------------------ //

    @Override
    public void onUnload() {
        /* no persistent resources to clean up */
    }
}
