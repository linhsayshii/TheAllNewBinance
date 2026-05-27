package com.auction.client.service.notification;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class NotificationCard extends HBox {

    private final Notification notification;
    private final Runnable onDismiss;

    public NotificationCard(Notification notification, Runnable onDismiss) {
        this.notification = notification;
        this.onDismiss = onDismiss;

        initializeView();
        applyAnimations();
    }

    private void initializeView() {
        this.getStyleClass().add("notification-card");
        this.getStyleClass().add(notification.getType().getStyleClass());

        Label iconLabel = new Label(notification.getType().getIconSymbol());
        iconLabel.getStyleClass().add("notification-icon");

        Label messageLabel = new Label(notification.getMessage());
        messageLabel.getStyleClass().add("notification-message");
        messageLabel.setWrapText(true);

        VBox textContainer = new VBox(messageLabel);
        HBox.setHgrow(textContainer, Priority.ALWAYS);

        Label closeBtn = new Label("×");
        closeBtn.getStyleClass().add("notification-close-btn");

        this.getChildren().addAll(iconLabel, textContainer, closeBtn);

        this.setOnMouseClicked(event -> dismissWithAnimation());
    }

    private void applyAnimations() {
        this.setOpacity(0.0);
        this.setTranslateY(-30);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), this);
        fadeIn.setToValue(1.0);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(250), this);
        slideIn.setToY(0.0);

        ParallelTransition entry = new ParallelTransition(fadeIn, slideIn);
        entry.play();
    }

    public void dismissWithAnimation() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), this);
        fadeOut.setToValue(0.0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(200), this);
        slideOut.setToY(-20.0);

        ParallelTransition exit = new ParallelTransition(fadeOut, slideOut);
        exit.setOnFinished(event -> Platform.runLater(onDismiss));
        exit.play();
    }
}
