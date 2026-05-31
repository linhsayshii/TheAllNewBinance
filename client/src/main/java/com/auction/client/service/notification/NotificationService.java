package com.auction.client.service.notification;

import com.auction.client.scene.NavigationService;
import com.auction.client.scene.SceneService;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class NotificationService {

    private static final NotificationService INSTANCE = new NotificationService();
    private static final int MAX_VISIBLE_NOTIFICATIONS = 3;
    private static final double AUTO_DISMISS_SECONDS = 5.0;

    private VBox container;
    private final List<ActiveNotificationHolder> activeHolders = new ArrayList<>();

    private NotificationService() {
        Platform.runLater(this::initContainer);
    }

    public static NotificationService getInstance() {
        return INSTANCE;
    }

    private void initContainer() {
        container = new VBox(10);
        container.setAlignment(Pos.TOP_CENTER);
        container.setPickOnBounds(false);
        container.getStyleClass().add("notification-container");
    }

    public void show(String message, NotificationType type) {
        if (message == null || type == null) {
            return;
        }

        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(message, type));
            return;
        }

        ensureAttachedToScene();

        if (activeHolders.size() >= MAX_VISIBLE_NOTIFICATIONS) {
            ActiveNotificationHolder oldest = activeHolders.get(0);
            oldest.card.dismissWithAnimation();
        }

        Notification notification = new Notification(message, type);
        ActiveNotificationHolder holder = new ActiveNotificationHolder(notification);

        NotificationCard card =
                new NotificationCard(notification, () -> removeNotification(holder));
        holder.setCard(card);

        activeHolders.add(holder);
        container.getChildren().add(card);

        PauseTransition autoDismiss = new PauseTransition(Duration.seconds(AUTO_DISMISS_SECONDS));
        autoDismiss.setOnFinished(
                event -> {
                    if (activeHolders.contains(holder)) {
                        card.dismissWithAnimation();
                    }
                });
        holder.setTimer(autoDismiss);
        autoDismiss.play();
    }

    private void removeNotification(ActiveNotificationHolder holder) {
        if (holder == null) {
            return;
        }

        if (holder.timer != null) {
            holder.timer.stop();
        }

        activeHolders.remove(holder);
        if (container != null && holder.card != null) {
            container.getChildren().remove(holder.card);
        }
    }

    private void ensureAttachedToScene() {
        NavigationService nav = NavigationService.getInstance();
        if (nav == null) {
            return;
        }

        SceneService sceneService = getSceneServiceRef();
        if (sceneService == null) {
            return;
        }

        StackPane sceneHost = sceneService.getSceneHost();
        if (sceneHost != null) {
            if (!sceneHost.getChildren().contains(container)) {
                sceneHost.getChildren().add(container);
                StackPane.setAlignment(container, Pos.TOP_CENTER);
            }
            container.toFront();
        }
    }

    private SceneService getSceneServiceRef() {
        try {
            java.lang.reflect.Field field =
                    NavigationService.class.getDeclaredField("sceneService");
            field.setAccessible(true);
            return (SceneService) field.get(NavigationService.getInstance());
        } catch (Exception e) {
            System.err.println(
                    "[NotificationService] Cannot access SceneService: " + e.getMessage());
            return null;
        }
    }

    private static class ActiveNotificationHolder {
        final Notification notification;
        NotificationCard card;
        PauseTransition timer;

        ActiveNotificationHolder(Notification notification) {
            this.notification = notification;
        }

        void setCard(NotificationCard card) {
            this.card = card;
        }

        void setTimer(PauseTransition timer) {
            this.timer = timer;
        }
    }
}
