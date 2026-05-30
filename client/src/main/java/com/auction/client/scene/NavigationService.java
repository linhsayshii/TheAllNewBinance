package com.auction.client.scene;

import com.auction.client.config.SceneRegistry;
import java.util.Map;

public class NavigationService {

    private static NavigationService instance;
    private final SceneService sceneService;

    public NavigationService(SceneService sceneService) {
        this.sceneService = sceneService;
        instance = this;
    }

    public static NavigationService getInstance() {
        return instance;
    }

    public void navigateTo(SceneRegistry sceneRegistry) {
        sceneService.switchTo(sceneRegistry);
    }

    /** Returns the controller of the currently displayed scene, or null if none. */
    public Object getCurrentController() {
        return sceneService.getCurrentController();
    }

    public void navigateTo(SceneRegistry sceneRegistry, Map<String, Object> data) {
        sceneService.switchTo(sceneRegistry, data);
    }

    public void openPopup(SceneRegistry sceneRegistry) {
        sceneService.openPopup(sceneRegistry);
    }

    public void closePopup() {
        sceneService.closePopup();
    }

    public boolean isPopupOpen() {
        return sceneService.isPopupOpen();
    }

    public void toggleTheme() {
        sceneService.toggleTheme();
    }

    public boolean isDarkTheme() {
        return sceneService.isDarkTheme();
    }
}
