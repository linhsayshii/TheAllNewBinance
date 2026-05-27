package com.auction.client.service.notification;

import java.util.UUID;

public class Notification {
    private final String id;
    private final String message;
    private final NotificationType type;
    private final long timestamp;

    public Notification(String message, NotificationType type) {
        this.id = UUID.randomUUID().toString();
        this.message = message;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public NotificationType getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
