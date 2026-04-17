package com.auction.client.scene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.auction.client.config.AppConfig;
import com.auction.client.config.SceneRegistry;
import com.auction.client.exception.SceneLoadException;
import com.auction.client.service.ResourceLoader;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneService {

    private final Stage stage;
    private final ResourceLoader resourceLoader;
    private final List<String> stylesheets;
    private SceneRegistry currentSceneRegistry;
    private final List<Scene> activeScenes = new ArrayList<>();
    private Object currentController;

    public SceneService(Stage stage, List<String> stylesheets) {
        this.stage = stage;
        this.stylesheets = stylesheets;
        this.resourceLoader = new ResourceLoader();
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
            
            Scene scene = new Scene(root);
            applyStylesheets(scene, false);
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