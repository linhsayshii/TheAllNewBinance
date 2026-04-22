package com.auction.client.unit.scene;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.auction.client.scene.SceneService;

import javafx.application.Platform;
import javafx.stage.Stage;

class SceneServiceTest {

    private static volatile boolean fxInitialized = false;

    @BeforeAll
    static void initFxToolkit() throws InterruptedException {
        if (fxInitialized) {
            return;
        }

        CountDownLatch startupLatch = new CountDownLatch(1);
        try {
            Platform.startup(startupLatch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            startupLatch.countDown();
        }

        if (!startupLatch.await(5, TimeUnit.SECONDS)) {
            Assertions.fail("Timed out while initializing JavaFX toolkit");
        }
        fxInitialized = true;
    }

    @Test
    void shouldCreateSceneService() throws InterruptedException {
        AtomicReference<SceneService> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                result.set(new SceneService(stage, List.of()));
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        if (!latch.await(5, TimeUnit.SECONDS)) {
            Assertions.fail("Timed out while waiting for JavaFX test execution");
        }
        if (error.get() != null) {
            Assertions.fail(error.get());
        }

        Assertions.assertNotNull(result.get());
    }
}