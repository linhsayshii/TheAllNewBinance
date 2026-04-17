package com.auction.client.service;

import java.io.InputStream;
import java.util.List;

import javafx.scene.text.Font;

public final class FontLoaderService {

    private static final List<String> FONT_RESOURCES = List.of(
        "/fonts/InriaSerif-Regular.ttf",
        "/fonts/InriaSerif-Bold.ttf",
        "/fonts/Montserrat-Regular.ttf",
        "/fonts/Montserrat-SemiBold.ttf",
        "/fonts/Montserrat-Bold.ttf"
    );

    private FontLoaderService() {
    }

    public static void preloadProjectFonts() {
        for (String fontPath : FONT_RESOURCES) {
            try (InputStream stream = FontLoaderService.class.getResourceAsStream(fontPath)) {
                if (stream == null) {
                    System.out.println("[Fonts] Missing font resource: " + fontPath);
                    continue;
                }   

                Font loaded = Font.loadFont(stream, 12);
                if (loaded == null) {
                    System.out.println("[Fonts] Could not parse font: " + fontPath);
                } else {
                    System.out.println(
                        "[Fonts] Loaded " + fontPath
                            + " | family='" + loaded.getFamily() + "'"
                            + " | name='" + loaded.getName() + "'"
                    );
                }
            } catch (Exception ex) {
                System.out.println("[Fonts] Failed to load font " + fontPath + ": " + ex.getMessage());
            }
        }
    }
}
