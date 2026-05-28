package com.auction.client.page.admin;

import com.auction.client.component.item.AdminAuctionCardController;
import com.auction.client.config.SceneRegistry;
import com.auction.client.scene.LifecycleAwareController;
import com.auction.client.scene.NavigationService;
import com.auction.client.service.NetworkService;
import com.auction.client.service.UserSessionService;
import com.auction.client.service.notification.NotificationService;
import com.auction.client.service.notification.NotificationType;
import com.auction.core.dto.auction.PromoteAuctionRequest;
import com.auction.core.dto.auction.PublicAuctionDto;
import com.auction.core.protocol.EventType;
import com.auction.core.users.User;
import com.auction.core.utils.JsonMapper;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for admin-page.fxml.
 *
 * <p>Two sections: 1. User Management — TableView listing all users (ID, username, email, role,
 * balance, status) 2. Listing Management — FlowPane of all auctions (Active/Pending/Sold/Unsold
 * tabs), each card has a [ Force Promote ] button (admin can set isFeatured without payment).
 *
 * <p>Security: All admin API calls are double-checked on the server by role. Client-side visibility
 * is for UX only.
 */
public class AdminPageController implements LifecycleAwareController {

    private static final String ADMIN_CARD_FXML = "/fxml/components/item/admin-auction-card.fxml";
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final long TIMEOUT_MS = 5_000;

    // ── Nav ──────────────────────────────────────────────────────────────
    @FXML private ToggleGroup navGroup;
    @FXML private ToggleButton navUsers;
    @FXML private ToggleButton navListings;

    // ── Content ──────────────────────────────────────────────────────────
    @FXML private StackPane contentArea;

    // ── Users section ────────────────────────────────────────────────────
    @FXML private VBox usersSection;
    @FXML private Label userCountLabel;
    @FXML private TableView<UserRow> usersTable;
    @FXML private TableColumn<UserRow, Integer> colUserId;
    @FXML private TableColumn<UserRow, String> colUsername;
    @FXML private TableColumn<UserRow, String> colEmail;
    @FXML private TableColumn<UserRow, String> colRole;
    @FXML private TableColumn<UserRow, String> colBalance;
    @FXML private TableColumn<UserRow, String> colUserStatus;

    // ── Listings section ─────────────────────────────────────────────────
    @FXML private VBox listingsSection;
    @FXML private Label listingCountLabel;
    @FXML private ToggleGroup listingTabGroup;
    @FXML private ToggleButton tabActive;
    @FXML private ToggleButton tabPending;
    @FXML private ToggleButton tabSold;
    @FXML private ToggleButton tabUnsold;
    @FXML private FlowPane listingsContainer;
    @FXML private Label listingsEmptyLabel;

    // ── Data ─────────────────────────────────────────────────────────────
    private final ObservableList<UserRow> userRows = FXCollections.observableArrayList();
    private final List<PublicAuctionDto> allAuctions = new ArrayList<>();
    private boolean dataLoaded = false;

    // ── Lifecycle ────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        // Guard: only ADMIN can use this page
        User currentUser = UserSessionService.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getRole() != User.Role.ADMIN) {
            NavigationService.getInstance().navigateTo(SceneRegistry.GENERAL_PAGE);
            return;
        }

        setupTableColumns();
        usersTable.setItems(userRows);

        navGroup.selectedToggleProperty()
                .addListener(
                        (obs, old, sel) -> {
                            if (sel == null) {
                                old.setSelected(true);
                                return;
                            }
                            showSection(sel);
                        });
        navUsers.setSelected(true);
        showSection(navUsers);
    }

    private void setupTableColumns() {
        colUserId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().id()));
        colUsername.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().username()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().email()));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().role()));
        colBalance.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().balance()));
        colUserStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status()));
    }

    private void showSection(Toggle sel) {
        usersSection.setVisible(sel == navUsers);
        usersSection.setManaged(sel == navUsers);
        listingsSection.setVisible(sel == navListings);
        listingsSection.setManaged(sel == navListings);

        if (!dataLoaded) {
            dataLoaded = true;
            loadDataAsync();
        } else {
            if (sel == navListings) {
                renderCurrentListingTab();
            }
        }
    }

    // ── Data loading ─────────────────────────────────────────────────────

    private void loadDataAsync() {
        java.util.concurrent.CompletableFuture.runAsync(
                () -> {
                    fetchUsers();
                    fetchAllAuctions();
                    Platform.runLater(
                            () -> {
                                userCountLabel.setText(userRows.size() + " users");
                                listingCountLabel.setText(allAuctions.size() + " auctions");
                                renderCurrentListingTab();
                            });
                });
    }

    @SuppressWarnings("unchecked")
    private void fetchUsers() {
        try {
            NetworkService ns = NetworkService.getInstance();
            CountDownLatch latch = new CountDownLatch(1);

            String corr = ns.sendRequest(EventType.GET_ALL_USERS_ADMIN, null);
            ns.addCorrelationHandler(
                    corr,
                    raw -> {
                        try {
                            Map<?, ?> resp = JsonMapper.fromJson(raw, Map.class);
                            if (Boolean.TRUE.equals(resp.get("success"))) {
                                Object data = resp.get("data");
                                if (data instanceof List<?> list) {
                                    for (Object entry : list) {
                                        String json = JsonMapper.toJson(entry);
                                        User u = JsonMapper.fromJson(json, User.class);
                                        if (u != null) {
                                            userRows.add(
                                                    new UserRow(
                                                            u.getId(),
                                                            u.getUsername() != null
                                                                    ? u.getUsername()
                                                                    : "—",
                                                            u.getEmail() != null
                                                                    ? u.getEmail()
                                                                    : "—",
                                                            u.getRole() != null
                                                                    ? u.getRole().name()
                                                                    : "USER",
                                                            "$"
                                                                    + MONEY_FMT.format(
                                                                            u.getBalance() != null
                                                                                    ? u.getBalance()
                                                                                            .doubleValue()
                                                                                    : 0.0),
                                                            "Active"));
                                        }
                                    }
                                }
                            }
                        } finally {
                            latch.countDown();
                        }
                    });

            latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @SuppressWarnings("unchecked")
    private void fetchAllAuctions() {
        try {
            NetworkService ns = NetworkService.getInstance();
            CountDownLatch latch = new CountDownLatch(1);

            String corr = ns.sendRequest(EventType.GET_ALL_AUCTIONS_ADMIN, null);
            ns.addCorrelationHandler(
                    corr,
                    raw -> {
                        try {
                            Map<?, ?> resp = JsonMapper.fromJson(raw, Map.class);
                            if (Boolean.TRUE.equals(resp.get("success"))) {
                                Object data = resp.get("data");
                                if (data instanceof List<?> list) {
                                    for (Object entry : list) {
                                        String json = JsonMapper.toJson(entry);
                                        PublicAuctionDto dto =
                                                JsonMapper.fromJson(json, PublicAuctionDto.class);
                                        if (dto != null) {
                                            allAuctions.add(dto);
                                        }
                                    }
                                }
                            }
                        } finally {
                            latch.countDown();
                        }
                    });

            latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ── Listing tabs ─────────────────────────────────────────────────────

    @FXML
    private void handleTabActive() {
        renderListings("ACTIVE");
    }

    @FXML
    private void handleTabPending() {
        renderListings("PENDING");
    }

    @FXML
    private void handleTabSold() {
        renderListings("ENDED");
    }

    @FXML
    private void handleTabUnsold() {
        renderListings("CANCELLED");
    }

    private void renderCurrentListingTab() {
        Toggle sel = listingTabGroup.getSelectedToggle();
        if (sel == tabPending) {
            renderListings("PENDING");
            return;
        }
        if (sel == tabSold) {
            renderListings("ENDED");
            return;
        }
        if (sel == tabUnsold) {
            renderListings("CANCELLED");
            return;
        }
        renderListings("ACTIVE");
    }

    private void renderListings(String statusFilter) {
        listingsContainer.getChildren().clear();
        boolean isPromotable = "ACTIVE".equals(statusFilter) || "PENDING".equals(statusFilter);

        List<PublicAuctionDto> filtered =
                allAuctions.stream()
                        .filter(dto -> statusFilter.equalsIgnoreCase(dto.getStatus()))
                        .toList();

        boolean empty = filtered.isEmpty();
        listingsEmptyLabel.setVisible(empty);
        listingsEmptyLabel.setManaged(empty);

        if (!empty) {
            for (PublicAuctionDto dto : filtered) {
                Runnable onForcePromote = isPromotable ? () -> forcePromote(dto) : null;
                Node card = loadAdminCard(dto, onForcePromote);
                if (card != null) {
                    listingsContainer.getChildren().add(card);
                }
            }
        }
    }

    // ── Force Promote ─────────────────────────────────────────────────────

    private void forcePromote(PublicAuctionDto dto) {
        PromoteAuctionRequest req = new PromoteAuctionRequest();
        req.setAuctionId(dto.getAuctionId());
        req.setPackageDays(3);
        req.setAdminForce(true);

        String corr = NetworkService.getInstance().sendRequest(EventType.PROMOTE_AUCTION, req);
        NetworkService.getInstance()
                .addCorrelationHandler(
                        corr,
                        raw ->
                                Platform.runLater(
                                        () -> {
                                            Map<?, ?> resp = JsonMapper.fromJson(raw, Map.class);
                                            boolean ok = Boolean.TRUE.equals(resp.get("success"));
                                            String msg =
                                                    ok
                                                            ? "Promoted: "
                                                                    + (dto.getItemName() != null
                                                                            ? dto.getItemName()
                                                                            : "Auction #"
                                                                                    + dto
                                                                                            .getAuctionId())
                                                            : "Promotion failed: "
                                                                    + resp.get("message");
                                            NotificationService.getInstance()
                                                    .show(
                                                            msg,
                                                            ok
                                                                    ? NotificationType.SUCCESS
                                                                    : NotificationType.ERROR);
                                            if (ok) {
                                                dto.setIsFeatured(true);
                                                // Refresh to update card state
                                                renderCurrentListingTab();
                                            }
                                        }));
    }

    // ── Card loader ────────────────────────────────────────────────────────

    private Node loadAdminCard(PublicAuctionDto dto, Runnable onForcePromote) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ADMIN_CARD_FXML));
            Node root = loader.load();
            AdminAuctionCardController ctrl = loader.getController();
            ctrl.setData(dto, onForcePromote);
            return root;
        } catch (IOException e) {
            System.err.println("[Admin] Failed to load admin-auction-card: " + e.getMessage());
            return null;
        }
    }

    // ── Nav handlers ──────────────────────────────────────────────────────

    @FXML
    private void handleNavUsers() {
        /* handled by listener */
    }

    @FXML
    private void handleNavListings() {
        /* handled by listener */
    }

    @FXML
    private void handleBackToProfile() {
        User user = UserSessionService.getInstance().getCurrentUser();
        if (user != null && user.getId() != null) {
            NavigationService.getInstance()
                    .navigateTo(SceneRegistry.PROFILE_PAGE, Map.of("userId", user.getId()));
        } else {
            NavigationService.getInstance().navigateTo(SceneRegistry.PROFILE_PAGE);
        }
    }

    @Override
    public void onUnload() {
        /* no cleanup needed */
    }

    // ── Inner data class ──────────────────────────────────────────────────

    public record UserRow(
            Integer id,
            String username,
            String email,
            String role,
            String balance,
            String status) {}
}
