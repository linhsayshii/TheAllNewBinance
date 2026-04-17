package com.auction.client.scene;

import com.auction.client.config.SceneRegistry;

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
}