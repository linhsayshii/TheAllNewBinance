package com.auction.client.unit.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.auction.client.service.CssDependencyIndex;

class CssDependencyIndexTest {

    @Test
    void shouldTrackImportedCssFilesRecursively() throws IOException {
        Path tempDir = Files.createTempDirectory("css-index-test");
        Path baseDir = Files.createDirectories(tempDir.resolve("base"));
        Path componentsDir = Files.createDirectories(tempDir.resolve("components"));

        Path appCss = tempDir.resolve("app.css");
        Path tokensCss = baseDir.resolve("tokens.css");
        Path cardCss = componentsDir.resolve("card.css");

        Files.writeString(appCss, "@import url(\"base/tokens.css\");\n@import url(\"components/card.css\");\n");
        Files.writeString(tokensCss, ":root { -fx-font-size: 14px; }\n");
        Files.writeString(cardCss, ".card { -fx-padding: 8px; }\n");

        CssDependencyIndex index = new CssDependencyIndex(appCss);

        Assertions.assertTrue(index.shouldReloadFor(appCss));
        Assertions.assertTrue(index.shouldReloadFor(tokensCss));
        Assertions.assertTrue(index.shouldReloadFor(cardCss));
    }
}
