package com.auction.client.scene;

import com.auction.client.config.AppConfig;
import com.auction.client.config.SceneRegistry;
import com.auction.client.exception.SceneLoadException;
import com.auction.client.service.ResourceLoader;
import com.auction.client.service.ThemeService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class SceneService {

    private final Stage stage;
    private final ResourceLoader resourceLoader;
    private final List<String> stylesheets;
    private final ThemeService themeService;
    private SceneRegistry currentSceneRegistry;
    private final List<Scene> activeScenes = new ArrayList<>();
    private Object currentController;
    private StackPane sceneHost;
    private StackPane modalOverlay;
    private StackPane modalDialog;
    private Object modalController;

    public SceneService(Stage stage, List<String> stylesheets) {
        this.stage = stage;
        this.stylesheets = stylesheets;
        this.resourceLoader = new ResourceLoader();
        this.themeService = ThemeService.getInstance();
    }

    public void switchTo(SceneRegistry sceneRegistry) {
        switchTo(sceneRegistry, null);
    }

    public void switchTo(SceneRegistry sceneRegistry, Map<String, Object> data) {
        try {
            FXMLLoader loader = new FXMLLoader(resourceLoader.requireUrl(sceneRegistry.fxmlPath()));
            Parent root = loader.load();

            // Clean up old controller
            if (currentController instanceof LifecycleAwareController) {
                ((LifecycleAwareController) currentController).onUnload();
            }

            // Keep reference to new controller
            currentController = loader.getController();

            // Pass navigation data to controller if it supports it
            if (data != null && currentController instanceof DataReceivable) {
                ((DataReceivable) currentController).onDataReceived(data);
            }

            closePopup();

            if (sceneHost == null) {
                sceneHost = new StackPane();
            }
            sceneHost.getChildren().setAll(root);

            // Reuse existing scene if sceneHost is already the root of one
            Scene scene = sceneHost.getScene();
            if (scene == null) {
                scene = new Scene(sceneHost);
                trackScene(scene);
            }
            applyStylesheets(scene, false);
            themeService.apply(scene, themeService.currentTheme());
            stage.setTitle(sceneRegistry.title());
            stage.setScene(scene);
            currentSceneRegistry = sceneRegistry;
        } catch (IOException e) {
            throw new SceneLoadException("Could not load scene " + sceneRegistry.name(), e);
        }
    }

    public SceneRegistry currentSceneRegistry() {
        return currentSceneRegistry;
    }

    public Scene currentScene() {
        return stage.getScene();
    }

    public List<Scene> activeScenes() {
        List<Scene> scenes = new ArrayList<>();
        for (Scene scene : activeScenes) {
            if (scene != null) {
                scenes.add(scene);
            }
        }
        Scene current = stage.getScene();
        if (current != null && !scenes.contains(current)) {
            scenes.add(current);
        }
        return scenes;
    }

    public void reloadStylesheets() {
        for (Scene scene : activeScenes()) {
            applyStylesheets(scene, true);
        }
    }

    public void reloadCurrentScene() {
        if (currentSceneRegistry != null) {
            switchTo(currentSceneRegistry);
        }
    }

    public void openPopup(SceneRegistry sceneRegistry) {
        if (sceneHost == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(resourceLoader.requireUrl(sceneRegistry.fxmlPath()));
            Parent popupRoot = loader.load();
            Object nextModalController = loader.getController();

            if (modalController instanceof LifecycleAwareController) {
                ((LifecycleAwareController) modalController).onUnload();
            }

            if (modalOverlay == null) {
                modalOverlay = new StackPane();
                modalOverlay.getStyleClass().add("app-modal-overlay");
                modalOverlay.setOnMouseClicked(event -> closePopup());
            }

            if (modalDialog == null) {
                modalDialog = new StackPane();
                modalDialog.getStyleClass().add("app-modal-dialog");
                modalDialog.setMinWidth(Region.USE_COMPUTED_SIZE);
                modalDialog.setPrefWidth(Region.USE_COMPUTED_SIZE);
                modalDialog.setMaxWidth(Region.USE_COMPUTED_SIZE);
                modalDialog.setMinHeight(Region.USE_COMPUTED_SIZE);
                modalDialog.setPrefHeight(Region.USE_COMPUTED_SIZE);
                modalDialog.setMaxHeight(Region.USE_COMPUTED_SIZE);
                modalDialog.setOnMouseClicked(event -> event.consume());
            }

            modalDialog.getChildren().setAll(popupRoot);
            modalOverlay.getChildren().setAll(modalDialog);
            if (!sceneHost.getChildren().contains(modalOverlay)) {
                sceneHost.getChildren().add(modalOverlay);
            }

            // Let popup content CSS dictate dialog size (e.g. login-card/register-card).
            syncModalSizeWithContent(popupRoot);
            Platform.runLater(() -> syncModalSizeWithContent(popupRoot));

            modalController = nextModalController;
        } catch (IOException e) {
            throw new SceneLoadException("Could not load popup scene " + sceneRegistry.name(), e);
        }
    }

    public void closePopup() {
        if (modalController instanceof LifecycleAwareController) {
            ((LifecycleAwareController) modalController).onUnload();
        }
        modalController = null;

        if (modalDialog != null) {
            modalDialog.getChildren().clear();
        }
        if (sceneHost != null && modalOverlay != null) {
            sceneHost.getChildren().remove(modalOverlay);
        }
    }

    public boolean isPopupOpen() {
        return sceneHost != null
                && modalOverlay != null
                && sceneHost.getChildren().contains(modalOverlay);
    }

    public void toggleTheme() {
        Scene current = currentScene();
        if (current == null) {
            return;
        }

        themeService.toggle(current);
    }

    public boolean isDarkTheme() {
        return themeService.currentTheme() == ThemeService.Theme.DARK;
    }

    private void applyStylesheets(Scene scene, boolean cacheBust) {
        for (String stylesheet : stylesheets) {
            String baseUrl = resourceLoader.requireUrl(stylesheet).toExternalForm();
            scene.getStylesheets()
                    .removeIf(
                            current ->
                                    current.equals(baseUrl) || current.startsWith(baseUrl + "?v="));
            scene.getStylesheets().add(withCacheBusting(baseUrl, cacheBust));
        }
    }

    private String withCacheBusting(String baseUrl, boolean cacheBust) {
        if (cacheBust && AppConfig.isHotReloadEnabled()) {
            return baseUrl + "?v=" + System.nanoTime();
        }
        return baseUrl;
    }

    private void trackScene(Scene scene) {
        if (scene == null || activeScenes.contains(scene)) {
            return;
        }
        activeScenes.add(scene);
    }

    private void syncModalSizeWithContent(Parent popupRoot) {
        popupRoot.applyCss();
        popupRoot.autosize();

        double width = popupRoot.prefWidth(-1);
        double height = popupRoot.prefHeight(-1);

        if (width <= 0 || Double.isNaN(width)) {
            width = popupRoot.getLayoutBounds().getWidth();
        }
        if (height <= 0 || Double.isNaN(height)) {
            height = popupRoot.getLayoutBounds().getHeight();
        }

        if (width > 0) {
            modalDialog.setMinWidth(width);
            modalDialog.setPrefWidth(width);
            modalDialog.setMaxWidth(width);
        }

        if (height > 0) {
            modalDialog.setMinHeight(height);
            modalDialog.setPrefHeight(height);
            modalDialog.setMaxHeight(height);
        }
    }
}
