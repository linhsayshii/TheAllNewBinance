package com.auction.client.app;

import com.auction.client.config.AppConfig;
import com.auction.client.config.SceneRegistry;
import com.auction.client.scene.NavigationService;
import com.auction.client.scene.SceneService;
import com.auction.client.service.FontLoaderService;
import com.auction.client.service.HotReloadService;

import javafx.application.Application;
import javafx.stage.Stage;

public class ClientApp extends Application {

    private HotReloadService hotReloadService;

    @Override
    public void start(Stage primaryStage) {
        FontLoaderService.preloadProjectFonts();

        SceneService sceneService = new SceneService(primaryStage, AppConfig.stylesheets());
        NavigationService navigationService = new NavigationService(sceneService);
        hotReloadService = new HotReloadService(sceneService);

        primaryStage.setWidth(AppConfig.defaultWidth());
        primaryStage.setHeight(AppConfig.defaultHeight());
        primaryStage.setMinWidth(AppConfig.minWidth());
        primaryStage.setMinHeight(AppConfig.minHeight());

        navigationService.navigateTo(SceneRegistry.GENERAL_PAGE);
        hotReloadService.start();
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (hotReloadService != null) {
            hotReloadService.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}