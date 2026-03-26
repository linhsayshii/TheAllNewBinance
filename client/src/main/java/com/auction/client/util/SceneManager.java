package com.auction.client.util;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.List;

public final class SceneManager {
	private static final String GENERAL_FXML_CLASSPATH = "/fxml/general.fxml";
	private static final String GENERAL_CSS_CLASSPATH = "/css/style.css";
	private static final List<Path> RESOURCE_ROOT_CANDIDATES = List.of(
		Paths.get("src/main/resources"),
		Paths.get("client/src/main/resources")
	);

	private static Timeline liveReloadTicker;
	private static Path devFxmlPath;
	private static Path devCssPath;
	private static FileTime lastFxmlModified;
	private static FileTime lastCssModified;

	private SceneManager() {
	}

	public static void showGeneral(Stage stage) {
		stage.setTitle("TheAllNewBinance - Client");
		stage.setMinWidth(1024);
		stage.setMinHeight(720);

		reloadGeneralScene(stage);
		stage.show();
		startLiveReload(stage);
		stage.setOnHidden(event -> stopLiveReload());
	}

	private static void reloadGeneralScene(Stage stage) {
		try {
			URL fxmlUrl = resolveResourceUrl("fxml/general.fxml", GENERAL_FXML_CLASSPATH);
			URL cssUrl = resolveResourceUrl("css/style.css", GENERAL_CSS_CLASSPATH);

			FXMLLoader loader = new FXMLLoader(fxmlUrl);
			Parent root = loader.load();

			Scene scene = new Scene(root, 1280, 800);
			if (cssUrl != null) {
				scene.getStylesheets().add(cssUrl.toExternalForm());
			}
			stage.setScene(scene);
			captureModifiedTimes();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot load " + GENERAL_FXML_CLASSPATH, e);
		}
	}

	private static URL resolveResourceUrl(String relativeResourcePath, String classpathResourcePath) {
		Path devPath = findDevResourcePath(relativeResourcePath);
		if (devPath != null) {
			if ("fxml/general.fxml".equals(relativeResourcePath)) {
				devFxmlPath = devPath;
			}
			if ("css/style.css".equals(relativeResourcePath)) {
				devCssPath = devPath;
			}
			try {
				return devPath.toUri().toURL();
			} catch (IOException e) {
				throw new IllegalStateException("Cannot resolve resource URL for " + devPath, e);
			}
		}

		URL classpathUrl = SceneManager.class.getResource(classpathResourcePath);
		if (classpathUrl == null && GENERAL_FXML_CLASSPATH.equals(classpathResourcePath)) {
			throw new IllegalStateException("Cannot find " + classpathResourcePath);
		}
		return classpathUrl;
	}

	private static Path findDevResourcePath(String relativeResourcePath) {
		for (Path rootCandidate : RESOURCE_ROOT_CANDIDATES) {
			Path candidate = rootCandidate.resolve(relativeResourcePath).toAbsolutePath().normalize();
			if (Files.exists(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	private static void startLiveReload(Stage stage) {
		if (!Boolean.parseBoolean(System.getProperty("ui.liveReload", "true"))) {
			return;
		}

		if (devFxmlPath == null && devCssPath == null) {
			return;
		}

		stopLiveReload();
		liveReloadTicker = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
			if (hasFileChanged(devFxmlPath, lastFxmlModified) || hasFileChanged(devCssPath, lastCssModified)) {
				reloadGeneralScene(stage);
			}
		}));
		liveReloadTicker.setCycleCount(Animation.INDEFINITE);
		liveReloadTicker.play();
	}

	private static void stopLiveReload() {
		if (liveReloadTicker != null) {
			liveReloadTicker.stop();
			liveReloadTicker = null;
		}
	}

	private static boolean hasFileChanged(Path path, FileTime previousModifiedTime) {
		if (path == null || !Files.exists(path)) {
			return false;
		}
		try {
			FileTime currentModifiedTime = Files.getLastModifiedTime(path);
			return previousModifiedTime == null || currentModifiedTime.compareTo(previousModifiedTime) > 0;
		} catch (IOException e) {
			return false;
		}
	}

	private static void captureModifiedTimes() {
		lastFxmlModified = readLastModified(devFxmlPath);
		lastCssModified = readLastModified(devCssPath);
	}

	private static FileTime readLastModified(Path path) {
		if (path == null || !Files.exists(path)) {
			return null;
		}
		try {
			return Files.getLastModifiedTime(path);
		} catch (IOException e) {
			return null;
		}
	}
}
