package com.auction.client.component.shell;

import com.auction.client.config.SceneRegistry;
import com.auction.client.scene.NavigationService;

import javafx.fxml.FXML;

public class HeaderComponentController {

	@FXML
	private void handleGoToGeneral() {
		NavigationService.getInstance().navigateTo(SceneRegistry.GENERAL_PAGE);
	}
}