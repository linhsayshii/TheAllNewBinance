package com.auction.client.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class ImageLoader {

    private static final int MAX_CACHE_SIZE = 100;
    private static final ConcurrentHashMap<String, Image> IMAGE_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<String> ACCESS_ORDER = new ConcurrentLinkedQueue<>();

    public static void loadImage(String imageUrl, StackPane imageContainer, Label imageLabel) {
        loadImage(imageUrl, imageContainer, imageLabel, 350);
    }

    public static void loadImage(
            String imageUrl, StackPane imageContainer, Label imageLabel, double targetWidth) {
        if (imageContainer == null) {
            return;
        }

        // Dọn sạch style cũ và các listener cũ bám trên Node này để tránh xung đột khi reuse Node
        imageContainer.setStyle("");
        String listenerKeyPrefix = "img-listener-";
        imageContainer
                .getProperties()
                .keySet()
                .removeIf(key -> key.toString().startsWith(listenerKeyPrefix));

        if (imageUrl != null && !imageUrl.isBlank()) {
            if (imageLabel != null) {
                imageLabel.setVisible(false);
            }

            String cacheKey = imageUrl + "@" + targetWidth;

            // 1. Lấy hoặc khởi tạo ảnh nguyên tử
            Image loadingImage;
            try {
                loadingImage =
                        IMAGE_CACHE.computeIfAbsent(
                                cacheKey,
                                key -> new Image(imageUrl, targetWidth, 0, true, true, true));
            } catch (IllegalArgumentException e) {
                // URL không hợp lệ (vd: "Image Url", text placeholder) → hiển thị fallback
                System.err.println("[ImageLoader] Invalid image URL: " + imageUrl);
                handleLoadFailed(imageContainer, imageLabel);
                return;
            }

            // 2. Cập nhật thứ tự LRU Cache
            updateCacheOrder(cacheKey);

            // 3. HANDLE IMAGE LIFECYCLE STATES
            if (loadingImage.isError()) {
                clearImageFromCache(cacheKey);
                handleLoadFailed(imageContainer, imageLabel);
            } else if (loadingImage.getProgress() == 1.0) {
                // CASE: IMAGE IS ALREADY FULLY LOADED (ImageView displays it immediately without
                // CSS override)
                applyImageToImageView(imageContainer, loadingImage);
            } else {
                // CASE: IMAGE IS STILL LOADING
                // Show placeholder background while waiting
                applyImageToImageView(imageContainer, loadingImage);

                final Image finalImg = loadingImage;
                ChangeListener<Number> progressListener =
                        new ChangeListener<>() {
                            @Override
                            public void changed(
                                    javafx.beans.value.ObservableValue<? extends Number> obs,
                                    Number old,
                                    Number current) {
                                if (current.doubleValue() == 1.0) {
                                    Platform.runLater(
                                            () -> {
                                                finalImg.progressProperty().removeListener(this);
                                                if (finalImg.isError()) {
                                                    clearImageFromCache(cacheKey);
                                                    if (isSameImage(imageContainer, finalImg)) {
                                                        handleLoadFailed(
                                                                imageContainer, imageLabel);
                                                    }
                                                } else {
                                                    applyImageToImageView(imageContainer, finalImg);
                                                }
                                            });
                                }
                            }
                        };

                loadingImage
                        .progressProperty()
                        .addListener(new WeakChangeListener<>(progressListener));
                imageContainer.getProperties().put(listenerKeyPrefix + cacheKey, progressListener);
            }

        } else {
            if (imageLabel != null) {
                imageLabel.setVisible(true);
                imageLabel.setText("No Image");
            }
            // Ẩn ImageView nếu có
            clearImageView(imageContainer);
            imageContainer.setBackground(null);
            imageContainer.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #1e3a5f, #0f172a);");
        }
    }

    private static void updateCacheOrder(String cacheKey) {
        if (!ACCESS_ORDER.contains(cacheKey)) {
            ACCESS_ORDER.add(cacheKey);
            while (ACCESS_ORDER.size() > MAX_CACHE_SIZE) {
                String oldestKey = ACCESS_ORDER.poll();
                if (oldestKey != null && !oldestKey.equals(cacheKey)) {
                    IMAGE_CACHE.remove(oldestKey);
                }
            }
        } else {
            ACCESS_ORDER.remove(cacheKey);
            ACCESS_ORDER.add(cacheKey);
        }
    }

    private static void clearImageFromCache(String cacheKey) {
        IMAGE_CACHE.remove(cacheKey);
        ACCESS_ORDER.remove(cacheKey);
    }

    private static void applyImageToImageView(StackPane container, Image image) {
        ImageView imageView = null;
        for (Node child : container.getChildren()) {
            if (child instanceof ImageView) {
                imageView = (ImageView) child;
                break;
            }
        }

        if (imageView == null) {
            imageView = new ImageView();
            imageView.setSmooth(true);
            imageView.setPreserveRatio(true);

            // Tự động co giãn theo kích thước của StackPane container
            imageView.fitWidthProperty().bind(container.widthProperty());
            imageView.fitHeightProperty().bind(container.heightProperty());

            // Thêm vào vị trí đầu tiên (dưới Label) để không che lấp text của Label
            container.getChildren().add(0, imageView);
        }

        imageView.setImage(image);
        imageView.setVisible(true);

        // Tránh bị CSS đè: đặt background của container thành trong suốt
        container.setBackground(null);
        container.setStyle("-fx-background-color: transparent;");
    }

    private static void clearImageView(StackPane container) {
        for (Node child : container.getChildren()) {
            if (child instanceof ImageView) {
                ((ImageView) child).setImage(null);
                child.setVisible(false);
            }
        }
    }

    private static boolean isSameImage(StackPane container, Image targetImage) {
        for (Node child : container.getChildren()) {
            if (child instanceof ImageView) {
                return ((ImageView) child).getImage() == targetImage;
            }
        }
        return false;
    }

    private static void handleLoadFailed(StackPane imageContainer, Label imageLabel) {
        if (imageLabel != null) {
            imageLabel.setVisible(true);
            imageLabel.setText("Failed to load");
        }
        clearImageView(imageContainer);
        imageContainer.setBackground(null);
        imageContainer.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #1e3a5f, #0f172a);");
    }
}
