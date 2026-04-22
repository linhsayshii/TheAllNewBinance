package com.auction.client.component.shell;

import org.kordamp.ikonli.javafx.FontIcon;

import com.auction.client.config.SceneRegistry;
import com.auction.client.scene.NavigationService;

import javafx.fxml.FXML;

public class HeaderComponentController {

	@FXML
	private FontIcon themeModeIcon;

	@FXML
	private void initialize() {
		refreshThemeModeIcon();
	}

	@FXML
	private void handleGoToGeneral() {
		NavigationService.getInstance().navigateTo(SceneRegistry.GENERAL_PAGE);
	}

	@FXML
	private void handleGoToLogin() {
		NavigationService.getInstance().openPopup(SceneRegistry.LOGIN_CARD);
	}

	@FXML
	private void handleGoToRegister() {
		NavigationService.getInstance().openPopup(SceneRegistry.REGISTER_CARD);
	}

	@FXML
	private void handleToggleTheme() {
		NavigationService.getInstance().toggleTheme();
		refreshThemeModeIcon();
	}

	private void refreshThemeModeIcon() {
		if (themeModeIcon == null) {
			return;
		}

		if (NavigationService.getInstance().isDarkTheme()) {
			themeModeIcon.setIconLiteral("fas-moon");
		} else {
			themeModeIcon.setIconLiteral("fas-sun");
		}
	}
}