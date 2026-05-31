package com.auction.client.service.notification;

public enum NotificationType {
    SUCCESS("success-badge", "✓"),
    WARNING("warning-badge", "⚠"),
    ERROR("error-badge", "✗"),
    INFO("info-badge", "ℹ");

    private final String styleClass;
    private final String iconSymbol;

    NotificationType(String styleClass, String iconSymbol) {
        this.styleClass = styleClass;
        this.iconSymbol = iconSymbol;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public String getIconSymbol() {
        return iconSymbol;
    }
}
