package com.auction.client.service;

import com.auction.client.config.AppConfig;
import javafx.scene.Scene;

public class ThemeService {

    private static final ThemeService INSTANCE = new ThemeService();

    public enum Theme {
        LIGHT("/css/themes/light.css"),
        DARK("/css/themes/dark.css");

        private final String stylesheet;

        Theme(String stylesheet) {
            this.stylesheet = stylesheet;
        }

        public String stylesheet() {
            return stylesheet;
        }
    }

    private final ResourceLoader resourceLoader = new ResourceLoader();
    private Theme currentTheme = Theme.LIGHT;

    private ThemeService() {}

    public static ThemeService getInstance() {
        return INSTANCE;
    }

    public Theme currentTheme() {
        return currentTheme;
    }

    public void apply(Scene scene, Theme theme) {
        if (scene == null || theme == null) {
            return;
        }

        String lightCss = resourceLoader.requireUrl(Theme.LIGHT.stylesheet()).toExternalForm();
        String darkCss = resourceLoader.requireUrl(Theme.DARK.stylesheet()).toExternalForm();
        removeStylesheet(scene, lightCss);
        removeStylesheet(scene, darkCss);

        String themeCss = resourceLoader.requireUrl(theme.stylesheet()).toExternalForm();
        scene.getStylesheets().add(withCacheBusting(themeCss));
        currentTheme = theme;
    }

    public void toggle(Scene scene) {
        if (currentTheme == Theme.LIGHT) {
            apply(scene, Theme.DARK);
        } else {
            apply(scene, Theme.LIGHT);
        }
    }

    public void reloadIfThemeChanged(Scene scene, String changedCssPath) {
        if (changedCssPath.endsWith("themes/light.css") && containsStylesheet(scene, Theme.LIGHT)) {
            apply(scene, Theme.LIGHT);
            return;
        }
        if (changedCssPath.endsWith("themes/dark.css") && containsStylesheet(scene, Theme.DARK)) {
            apply(scene, Theme.DARK);
        }
    }

    private boolean containsStylesheet(Scene scene, Theme theme) {
        String baseUrl = resourceLoader.requireUrl(theme.stylesheet()).toExternalForm();
        return scene.getStylesheets().stream()
                .anyMatch(
                        stylesheet ->
                                stylesheet.equals(baseUrl)
                                        || stylesheet.startsWith(baseUrl + "?v="));
    }

    private void removeStylesheet(Scene scene, String baseUrl) {
        scene.getStylesheets()
                .removeIf(
                        stylesheet ->
                                stylesheet.equals(baseUrl)
                                        || stylesheet.startsWith(baseUrl + "?v="));
    }

    private String withCacheBusting(String baseUrl) {
        if (AppConfig.isHotReloadEnabled()) {
            return baseUrl + "?v=" + System.nanoTime();
        }
        return baseUrl;
    }
}
