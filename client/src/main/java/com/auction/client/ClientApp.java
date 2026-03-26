package com.auction.client;

import com.auction.client.util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class ClientApp extends Application {

	@Override
	public void start(Stage primaryStage) {
		SceneManager.showGeneral(primaryStage);
	}

	public static void main(String[] args) {
		launch(args);
	}
}
