package com.auction.client.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public final class AppConfig {

    private static final String DEV_MODE_KEY = "app.devMode";
    private static final String HOT_RELOAD_KEY = "app.hotReload";
    private static final Optional<Path> SOURCE_RESOURCE_ROOT = detectSourceResourceRoot();

    private AppConfig() {
    }

    public static double defaultWidth() {
        return 1280;
    }

    public static double defaultHeight() {
        return 800;
    }

    public static double minWidth() {
        return 1024;
    }

    public static double minHeight() {
        return 640;
    }

    public static List<String> stylesheets() {
        return List.of("/css/app.css");
    }

    public static boolean isDevMode() {
        return Boolean.parseBoolean(System.getProperty(DEV_MODE_KEY, "false"));
    }

    public static boolean isHotReloadEnabled() {
        return isDevMode() && Boolean.parseBoolean(System.getProperty(HOT_RELOAD_KEY, "false"));
    }

    public static boolean shouldUseSourceResources() {
        return isDevMode() && SOURCE_RESOURCE_ROOT.isPresent();
    }

    public static Optional<Path> sourceResourceRoot() {
        return SOURCE_RESOURCE_ROOT;
    }

    private static Optional<Path> detectSourceResourceRoot() {
        Path cwd = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        Path[] candidates = new Path[] {
            cwd.resolve("src/main/resources"),
            cwd.resolve("client/src/main/resources")
        };

        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }
}