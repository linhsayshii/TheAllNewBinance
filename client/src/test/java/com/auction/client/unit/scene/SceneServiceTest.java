package com.auction.client.unit.scene;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.auction.client.scene.SceneService;

import javafx.stage.Stage;

class SceneServiceTest {

    @Test
    void shouldCreateSceneService() {
        Stage stage = new Stage();
        SceneService sceneService = new SceneService(stage, List.of());
        Assertions.assertNotNull(sceneService);
    }
}