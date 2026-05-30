package com.auction.client.service;

import com.auction.client.config.AppConfig;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class ResourceLoader {

    public URL requireUrl(String path) {
        if (AppConfig.shouldUseSourceResources() && isReloadablePath(path)) {
            URL sourceUrl = fromSourceResources(path);
            if (sourceUrl != null) {
                return sourceUrl;
            }
        }

        URL url = getClass().getResource(path);
        return Objects.requireNonNull(url, "Resource not found: " + path);
    }

    private boolean isReloadablePath(String path) {
        return path.startsWith("/css/") || path.startsWith("/fxml/");
    }

    private URL fromSourceResources(String classpathPath) {
        return AppConfig.sourceResourceRoot()
                .map(root -> root.resolve(trimLeadingSlash(classpathPath)))
                .filter(Files::exists)
                .map(this::toUrl)
                .orElse(null);
    }

    private URL toUrl(Path file) {
        try {
            return file.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Could not create URL for " + file, e);
        }
    }

    private String trimLeadingSlash(String path) {
        if (path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }
}
