package com.auction.client.scene;

import com.auction.client.config.SceneRegistry;

public class NavigationService {

    private final SceneService sceneService;

    public NavigationService(SceneService sceneService) {
        this.sceneService = sceneService;
    }

    public void navigateTo(SceneRegistry sceneRegistry) {
        sceneService.switchTo(sceneRegistry);
    }
}