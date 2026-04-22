package com.auction.client.scene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.auction.client.config.AppConfig;
import com.auction.client.config.SceneRegistry;
import com.auction.client.exception.SceneLoadException;
import com.auction.client.service.ResourceLoader;
import com.auction.client.service.ThemeService;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
        try {
            FXMLLoader loader = new FXMLLoader(resourceLoader.requireUrl(sceneRegistry.fxmlPath()));
            Parent root = loader.load();
            
            // Clean up old controller
            if (currentController instanceof LifecycleAwareController) {
                ((LifecycleAwareController) currentController).onUnload();
            }
            
            // Keep reference to new controller
            currentController = loader.getController();

            closePopup();

            if (sceneHost == null) {
                sceneHost = new StackPane();
            }
            sceneHost.getChildren().setAll(root);
            
            Scene scene = new Scene(sceneHost);
            applyStylesheets(scene, false);
            themeService.apply(scene, themeService.currentTheme());
            stage.setTitle(sceneRegistry.title());
            stage.setScene(scene);
            currentSceneRegistry = sceneRegistry;
            trackScene(scene);
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
                modalDialog.setPrefWidth(500);
                modalDialog.setMinWidth(500);
                modalDialog.setMaxWidth(500);
                modalDialog.setOnMouseClicked(event -> event.consume());
            }

            modalDialog.getChildren().setAll(popupRoot);
            modalOverlay.getChildren().setAll(modalDialog);
            if (!sceneHost.getChildren().contains(modalOverlay)) {
                sceneHost.getChildren().add(modalOverlay);
            }

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
        return sceneHost != null && modalOverlay != null && sceneHost.getChildren().contains(modalOverlay);
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
            scene.getStylesheets().removeIf(current -> current.equals(baseUrl) || current.startsWith(baseUrl + "?v="));
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
}