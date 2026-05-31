package com.auction.client.service;

import com.auction.core.users.User;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Holds the authenticated user's data for the current session. Use login(user) after a successful
 * LOGIN response and logout() on sign-out.
 */
public class UserSessionService {

    private static UserSessionService instance;

    private ObjectProperty<User> currentUserProperty = new SimpleObjectProperty<>();

    private UserSessionService() {
        try {
            com.auction.client.service.NetworkService.getInstance()
                    .getClient()
                    .addResponseHandler(
                            com.auction.core.protocol.EventType.BALANCE_UPDATE,
                            "UserSessionService",
                            message -> {
                                try {
                                    java.util.Map<?, ?> node =
                                            com.auction.core.utils.JsonMapper.fromJson(
                                                    message, java.util.Map.class);
                                    if (node != null && Boolean.TRUE.equals(node.get("success"))) {
                                        java.util.Map<?, ?> data =
                                                (java.util.Map<?, ?>) node.get("data");
                                        if (data != null) {
                                            javafx.application.Platform.runLater(
                                                    () -> {
                                                        User current = getCurrentUser();
                                                        if (current != null) {
                                                            Object balObj = data.get("balance");
                                                            Object lockObj =
                                                                    data.get("lockedBalance");
                                                            java.math.BigDecimal bal =
                                                                    balObj instanceof Number n
                                                                            ? java.math.BigDecimal
                                                                                    .valueOf(
                                                                                            n
                                                                                                    .doubleValue())
                                                                            : null;
                                                            java.math.BigDecimal lock =
                                                                    lockObj instanceof Number n
                                                                            ? java.math.BigDecimal
                                                                                    .valueOf(
                                                                                            n
                                                                                                    .doubleValue())
                                                                            : null;
                                                            current.syncFinancialState(bal, lock);
                                                            currentUserProperty.set(null);
                                                            currentUserProperty.set(current);
                                                        }
                                                    });
                                        }
                                    }
                                } catch (Exception e) {
                                    System.err.println(
                                            "[UserSessionService] Failed to parse BALANCE_UPDATE: "
                                                    + e.getMessage());
                                }
                            });

            com.auction.client.service.NetworkService.getInstance()
                    .getClient()
                    .addResponseHandler(
                            com.auction.core.protocol.EventType.FORCE_LOGOUT_ADMIN,
                            "UserSessionServiceForceLogout",
                            message -> javafx.application.Platform.runLater(
                                    () -> {
                                        currentUserProperty.set(null);
                                        if (com.auction.client.scene.NavigationService
                                                .getInstance()
                                                .isPopupOpen()) {
                                            com.auction.client.scene.NavigationService
                                                    .getInstance()
                                                    .closePopup();
                                        }
                                        com.auction.client.scene.NavigationService
                                                .getInstance()
                                                .navigateTo(
                                                        com.auction.client.config.SceneRegistry
                                                                .GENERAL_PAGE);
                                        com.auction.client.service.notification
                                                .NotificationService.getInstance()
                                                .show(
                                                        "Tài khoản của bạn đã bị vô hiệu hóa bởi Admin.",
                                                        com.auction.client.service.notification
                                                                .NotificationType.ERROR);
                                    }));
        } catch (Exception e) {
            System.err.println(
                    "[UserSessionService] Failed to register BALANCE_UPDATE handler: "
                            + e.getMessage());
        }
    }

    public static synchronized UserSessionService getInstance() {
        if (instance == null) {
            instance = new UserSessionService();
        }
        return instance;
    }

    public boolean isAuthenticated() {
        return currentUserProperty.get() != null;
    }

    public User getCurrentUser() {
        return currentUserProperty.get();
    }

    public ObjectProperty<User> currentUserProperty() {
        return currentUserProperty;
    }

    /** Call after receiving a successful LOGIN/REGISTER response from the server. */
    public void login(User user) {
        this.currentUserProperty.set(user);
    }

    public void logout() {
        try {
            if (isAuthenticated()) {
                Integer userId = getCurrentUser().getId();
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                if (userId != null) {
                    payload.put("userId", userId);
                }
                com.auction.client.service.NetworkService.getInstance()
                        .sendRequest(com.auction.core.protocol.EventType.LOGOUT, payload);
            }
        } catch (Exception e) {
            System.err.println("Error during network logout: " + e.getMessage());
        } finally {
            this.currentUserProperty.set(null);
        }
    }
}
