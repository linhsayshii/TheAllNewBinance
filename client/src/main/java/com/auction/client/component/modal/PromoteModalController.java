package com.auction.client.component.modal;

import com.auction.client.service.NetworkService;
import com.auction.client.service.UserSessionService;
import com.auction.core.dto.auction.PromoteAuctionRequest;
import com.auction.core.protocol.EventType;
import com.auction.core.utils.JsonMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

/**
 * Controller cho promote-modal.fxml.
 *
 * <p>Flow: 1. Gọi {@link #open(StackPane, Integer, String, Runnable)} để hiển thị modal lên overlay
 * (StackPane của personal-profile-page). 2. User chọn gói 1 ngày / 3 ngày → tính toán ngày kết thúc
 * và chi phí. 3. Bấm "Thanh toán" → gửi PROMOTE_AUCTION request → hiển thị kết quả. 4. Bấm "Huỷ"
 * hoặc click overlay → đóng modal, gọi onSuccess callback nếu thành công.
 */
public class PromoteModalController {

    private static final double PRICE_1_DAY = 100.0;
    private static final double PRICE_3_DAYS = 250.0;
    private static final DateTimeFormatter END_DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 00:00");
    private static final int MAX_DESC_LENGTH = 200;

    @FXML private Label listingNameLabel;
    @FXML private ToggleGroup packageGroup;
    @FXML private ToggleButton pkg1DayBtn;
    @FXML private ToggleButton pkg3DayBtn;
    @FXML private Label estimatedEndLabel;
    @FXML private TextArea descriptionArea;
    @FXML private Label costLabel;
    @FXML private Label balanceLabel;
    @FXML private Button confirmBtn;
    @FXML private Label resultLabel;

    private Integer auctionId;
    private StackPane hostOverlay; // the StackPane in personal-profile-page that we are placed in
    private javafx.scene.Node rootNode; // the loaded root FXML node (StackPane overlay)
    private Runnable onSuccessCallback;
    private boolean submitted = false;

    // ── FXML Lifecycle ────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        // Limit description to 200 chars
        descriptionArea
                .textProperty()
                .addListener(
                        (obs, old, val) -> {
                            if (val != null && val.length() > MAX_DESC_LENGTH) {
                                descriptionArea.setText(val.substring(0, MAX_DESC_LENGTH));
                            }
                        });

        // Default to 1-day package
        pkg1DayBtn.setSelected(true);
        refreshPackageInfo();
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Opens the promote modal over the given host StackPane.
     *
     * @param host StackPane of the profile page that will contain the overlay
     * @param loadedRoot root Node returned by FXMLLoader.load()
     * @param auctionId auction to promote
     * @param listingTitle display name of the listing
     * @param onSuccess callback invoked on the FX thread after successful promotion
     */
    public void open(
            StackPane host,
            javafx.scene.Node loadedRoot,
            Integer auctionId,
            String listingTitle,
            Runnable onSuccess) {
        this.hostOverlay = host;
        this.rootNode = loadedRoot;
        this.auctionId = auctionId;
        this.onSuccessCallback = onSuccess;
        this.submitted = false;

        listingNameLabel.setText(listingTitle != null ? listingTitle : "Auction #" + auctionId);
        pkg1DayBtn.setSelected(true);
        descriptionArea.clear();
        resultLabel.setVisible(false);
        resultLabel.setManaged(false);
        confirmBtn.setDisable(false);

        // Show current balance
        double balance = 0.0;
        var user = UserSessionService.getInstance().getCurrentUser();
        if (user != null && user.getBalance() != null) {
            double rawBalance = user.getBalance().doubleValue();
            double rawLocked =
                    user.getLockedBalance() != null ? user.getLockedBalance().doubleValue() : 0.0;
            balance = rawBalance - rawLocked;
        }
        balanceLabel.setText(String.format("$%,.2f", balance));

        refreshPackageInfo();

        // Inject into host overlay
        if (!host.getChildren().contains(rootNode)) {
            host.getChildren().add(rootNode);
        }
        rootNode.setVisible(true);
        rootNode.setManaged(true);
        rootNode.toFront();
    }

    // ── Package selection ─────────────────────────────────────────────────

    @FXML
    private void handlePackageChange() {
        refreshPackageInfo();
    }

    private void refreshPackageInfo() {
        int days = selectedDays();
        double cost = (days == 1) ? PRICE_1_DAY : PRICE_3_DAYS;
        costLabel.setText(String.format("$%,.0f", cost));

        // featuredUntil = 00:00 of (tomorrow + days - 1) = end of day (days) from today
        LocalDate expiry = LocalDate.now().plusDays(days + 1);
        estimatedEndLabel.setText(expiry.format(END_DATE_FMT));
    }

    private int selectedDays() {
        if (pkg3DayBtn.isSelected()) {
            return 3;
        }
        return 1;
    }

    // ── Actions ──────────────────────────────────────────────────────────

    @FXML
    private void handleConfirm() {
        if (submitted || auctionId == null) {
            return;
        }
        submitted = true;
        confirmBtn.setDisable(true);

        PromoteAuctionRequest req = new PromoteAuctionRequest();
        req.setAuctionId(auctionId);
        req.setPackageDays(selectedDays());
        String desc = descriptionArea.getText().strip();
        req.setShortDescription(desc.isBlank() ? null : desc);

        String corr = NetworkService.getInstance().sendRequest(EventType.PROMOTE_AUCTION, req);
        NetworkService.getInstance()
                .addCorrelationHandler(corr, raw -> Platform.runLater(() -> handleResponse(raw)));
    }

    @FXML
    private void handleClose() {
        close(false);
    }

    @FXML
    private void handleOverlayClick(MouseEvent e) {
        // Only close if clicking on the dim backdrop, not the box
        close(false);
    }

    /** Called on VBox so click doesn't propagate to overlay */
    @FXML
    private void consumeClick(MouseEvent e) {
        e.consume();
    }

    // ── Response ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void handleResponse(String raw) {
        try {
            Map<?, ?> root = JsonMapper.fromJson(raw, Map.class);
            boolean ok = Boolean.TRUE.equals(root.get("success"));

            if (ok) {
                showResult(
                        true,
                        "✓ Promotion successful! Your listing will now appear on the home page.");
                if (onSuccessCallback != null) {
                    // slight delay to let user read the success message, then close
                    new Thread(
                                    () -> {
                                        try {
                                            Thread.sleep(1800);
                                        } catch (InterruptedException ignored) {
                                        }
                                        Platform.runLater(
                                                () -> {
                                                    close(true);
                                                    onSuccessCallback.run();
                                                });
                                    })
                            .start();
                }
            } else {
                String msg =
                        root.get("message") instanceof String s
                                ? s
                                : "Promotion failed, please try again.";
                showResult(false, "✗ " + msg);
                confirmBtn.setDisable(false);
                submitted = false;
            }
        } catch (Exception e) {
            showResult(false, "✗ Unknown error. Please try again.");
            confirmBtn.setDisable(false);
            submitted = false;
        }
    }

    private void showResult(boolean success, String msg) {
        resultLabel.setText(msg);
        resultLabel.getStyleClass().removeIf(c -> c.startsWith("pm-result-"));
        resultLabel.getStyleClass().add(success ? "pm-result-ok" : "pm-result-err");
        resultLabel.setVisible(true);
        resultLabel.setManaged(true);
    }

    private void close(boolean wasSuccess) {
        if (hostOverlay != null && rootNode != null) {
            hostOverlay.getChildren().remove(rootNode);
        }
    }
}
