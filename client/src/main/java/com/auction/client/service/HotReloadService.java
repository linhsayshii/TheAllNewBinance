package com.auction.client.service;

import com.auction.client.config.AppConfig;
import com.auction.client.scene.SceneService;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Scene;

public class HotReloadService {

    private static final long DEBOUNCE_MS = 150;

    private final SceneService sceneService;
    private final ThemeService themeService;
    private final Map<WatchKey, Path> watchDirectories = new HashMap<>();

    private WatchService watchService;
    private Thread watcherThread;
    private volatile boolean running;
    private CssDependencyIndex cssDependencyIndex;

    public HotReloadService(SceneService sceneService) {
        this.sceneService = sceneService;
        this.themeService = ThemeService.getInstance();
    }

    public void start() {
        if (running) {
            return;
        }

        if (!AppConfig.isHotReloadEnabled()) {
            System.out.println(
                    "[HotReload] Disabled. Set app.devMode=true and app.hotReload=true to enable.");
            return;
        }

        Path resourceRoot = AppConfig.sourceResourceRoot().orElse(null);
        if (resourceRoot == null) {
            System.out.println(
                    "[HotReload] Source resource root was not found. Expected src/main/resources.");
            return;
        }

        Path cssRoot = resourceRoot.resolve("css");
        Path fxmlRoot = resourceRoot.resolve("fxml");
        Path appCss = cssRoot.resolve("app.css");
        this.cssDependencyIndex = new CssDependencyIndex(appCss);

        try {
            watchService = FileSystems.getDefault().newWatchService();
            registerAllDirectories(cssRoot);
            registerAllDirectories(fxmlRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize hot reload watcher", e);
        }

        running = true;
        watcherThread = new Thread(this::watchLoop, "client-hot-reload");
        watcherThread.setDaemon(true);
        watcherThread.start();
        System.out.println("[HotReload] Watching resources under: " + resourceRoot);
    }

    public void stop() {
        running = false;
        if (watcherThread != null) {
            watcherThread.interrupt();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {
                // Nothing else to do on shutdown.
            }
        }
    }

    private void watchLoop() {
        while (running) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException | RuntimeException e) {
                Thread.currentThread().interrupt();
                return;
            }

            Path directory = watchDirectories.get(key);
            if (directory == null) {
                key.reset();
                continue;
            }

            List<Path> changedFiles = new ArrayList<>();
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }
                Path file = directory.resolve((Path) event.context()).normalize();
                changedFiles.add(file);
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE
                        && Files.isDirectory(file)) {
                    registerAllDirectories(file);
                }
            }
            key.reset();

            if (!changedFiles.isEmpty()) {
                sleepDebounce();
                handleChanges(changedFiles);
            }
        }
    }

    private void handleChanges(List<Path> changedFiles) {
        boolean shouldReloadCss = false;
        boolean shouldReloadFxml = false;

        for (Path changedFile : changedFiles) {
            String normalizedPath = changedFile.toString().replace('\\', '/');
            if (normalizedPath.endsWith(".css")) {
                cssDependencyIndex.rebuild();
                if (cssDependencyIndex.shouldReloadFor(changedFile)) {
                    shouldReloadCss = true;
                }
            }
            if (normalizedPath.endsWith(".fxml") && isCurrentSceneFxml(normalizedPath)) {
                shouldReloadFxml = true;
            }
        }

        if (!shouldReloadCss && !shouldReloadFxml) {
            return;
        }

        final boolean reloadCss = shouldReloadCss;
        final boolean reloadFxml = shouldReloadFxml;
        Platform.runLater(
                () -> {
                    if (reloadCss) {
                        sceneService.reloadStylesheets();
                        reloadThemeIfNeeded(changedFiles);
                    }
                    if (reloadFxml) {
                        sceneService.reloadCurrentScene();
                    }
                });
    }

    private void reloadThemeIfNeeded(List<Path> changedFiles) {
        for (Path changedFile : changedFiles) {
            String normalizedPath = changedFile.toString().replace('\\', '/');
            if (normalizedPath.endsWith("themes/light.css")
                    || normalizedPath.endsWith("themes/dark.css")) {
                for (Scene scene : sceneService.activeScenes()) {
                    themeService.reloadIfThemeChanged(scene, normalizedPath);
                }
            }
        }
    }

    private boolean isCurrentSceneFxml(String normalizedPath) {
        var current = sceneService.currentSceneRegistry();
        if (current == null) {
            return false;
        }
        String expectedSuffix =
                current.fxmlPath().startsWith("/")
                        ? current.fxmlPath().substring(1)
                        : current.fxmlPath();
        return normalizedPath.endsWith(expectedSuffix);
    }

    private void registerAllDirectories(Path rootDirectory) {
        if (rootDirectory == null || !Files.exists(rootDirectory)) {
            return;
        }

        try {
            Files.walk(rootDirectory).filter(Files::isDirectory).forEach(this::registerDirectory);
        } catch (IOException ignored) {
            // Ignore one broken branch and continue watching available directories.
        }
    }

    private void registerDirectory(Path directory) {
        try {
            WatchKey key =
                    directory.register(
                            watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_DELETE);
            watchDirectories.put(key, directory);
        } catch (IOException ignored) {
            // Ignore directory that cannot be watched.
        }
    }

    private void sleepDebounce() {
        try {
            TimeUnit.MILLISECONDS.sleep(DEBOUNCE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
