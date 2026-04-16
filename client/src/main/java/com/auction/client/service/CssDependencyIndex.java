package com.auction.client.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CssDependencyIndex {

    private static final Pattern IMPORT_PATTERN = Pattern.compile("@import\\s+(?:url\\()?['\"]?([^'\")]+)['\"]?\\)?");

    private final Path appCssPath;
    private final Set<Path> dependencies = new HashSet<>();

    public CssDependencyIndex(Path appCssPath) {
        this.appCssPath = appCssPath.toAbsolutePath().normalize();
        rebuild();
    }

    public synchronized void rebuild() {
        dependencies.clear();
        collectDependencies(appCssPath, dependencies);
    }

    public synchronized boolean shouldReloadFor(Path changedPath) {
        Path normalized = changedPath.toAbsolutePath().normalize();
        return dependencies.contains(normalized);
    }

    private void collectDependencies(Path cssPath, Set<Path> visited) {
        if (!Files.exists(cssPath) || visited.contains(cssPath)) {
            return;
        }

        visited.add(cssPath);

        try {
            for (String line : Files.readAllLines(cssPath)) {
                Matcher matcher = IMPORT_PATTERN.matcher(line);
                while (matcher.find()) {
                    String importPath = matcher.group(1).trim();
                    if (importPath.isEmpty() || importPath.startsWith("http://") || importPath.startsWith("https://")) {
                        continue;
                    }
                    Path imported = cssPath.getParent().resolve(importPath).normalize();
                    collectDependencies(imported, visited);
                }
            }
        } catch (IOException ignored) {
            // Ignore transient parse errors while file is being written by editor.
        }
    }
}
