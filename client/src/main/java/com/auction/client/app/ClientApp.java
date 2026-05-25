package com.auction.client.app;

import com.auction.client.config.AppConfig;
import com.auction.client.config.SceneRegistry;
import com.auction.client.mock.MockDataProvider;
import com.auction.client.scene.NavigationService;
import com.auction.client.scene.SceneService;
import com.auction.client.service.FontLoaderService;
import com.auction.client.service.HotReloadService;
import com.auction.client.service.NetworkService;
import com.auction.client.service.UserSessionService;
import com.auction.core.products.attribute.LuxuryAttributes;
import com.auction.core.products.factory.ItemFactoryProvider;
import com.auction.core.users.User;

import javafx.application.Application;
import javafx.stage.Stage;

public class ClientApp extends Application {

    private HotReloadService hotReloadService;

    @Override
    public void start(Stage primaryStage) {
        // 1. Ép JVM load lớp LuxuryAttributes an toàn với Try-Catch
        try {
            Class.forName(LuxuryAttributes.class.getName());
            System.out.println("[ClientApp] LuxuryAttributes class loaded – KEY_POOL pre-populated.");
        } catch (ClassNotFoundException e) {
            System.err.println("[ClientApp] Failed to pre-load LuxuryAttributes: " + e.getMessage());
        }

        // 2. Quét SPI để đóng băng registry các Factory cho Item
        ItemFactoryProvider.initialize();

        // Init network FIRST — controllers register handlers on initialize()
        NetworkService.init("ws://localhost:8080");

        // ── Mock Mode ──────────────────────────────────────────────────
        if (AppConfig.isMockMode()) {
            printMockBanner();
            // Auto-login the default mock user so header + profile work
            MockDataProvider provider = NetworkService.getInstance().getMockDataProvider();
            if (provider != null) {
                User defaultUser = provider.getDefaultUser();
                if (defaultUser != null) {
                    UserSessionService.getInstance().login(defaultUser);
                    System.out.println(
                            "[MockMode] Auto-logged in as: "
                                    + defaultUser.getUsername()
                                    + " (id="
                                    + defaultUser.getId()
                                    + ")");
                }
            }
        }
        // ──────────────────────────────────────────────────────────────

        FontLoaderService.preloadProjectFonts();

        SceneService sceneService = new SceneService(primaryStage, AppConfig.stylesheets());
        NavigationService navigationService = new NavigationService(sceneService);
        hotReloadService = new HotReloadService(sceneService);

        primaryStage.setWidth(AppConfig.defaultWidth());
        primaryStage.setHeight(AppConfig.defaultHeight());
        primaryStage.setMinWidth(AppConfig.minWidth());
        primaryStage.setMinHeight(AppConfig.minHeight());
        if (AppConfig.isMockMode()) {
            primaryStage.setTitle("TheAllNewBinance [MOCK MODE]");
        }

        navigationService.navigateTo(SceneRegistry.GENERAL_PAGE);
        hotReloadService.start();
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (hotReloadService != null) {
            hotReloadService.stop();
        }
        NetworkService.getInstance().shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static void printMockBanner() {
        System.out.println(
                """

            ╔══════════════════════════════════════════════════════╗
            ║           ⚠  MOCK MODE ACTIVE  ⚠                    ║
            ║  No server connection. Using local mock data.        ║
            ║  Run with: mvn javafx:run -pl client -Pmock          ║
            ╚══════════════════════════════════════════════════════╝
                """);
    }
}
