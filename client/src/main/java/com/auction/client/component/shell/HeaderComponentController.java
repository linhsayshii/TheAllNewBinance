package com.auction.client.component.shell;

import org.kordamp.ikonli.javafx.FontIcon;

import com.auction.client.config.SceneRegistry;
import com.auction.client.scene.NavigationService;

import javafx.fxml.FXML;

public class HeaderComponentController {

	@FXML
	private FontIcon themeModeIcon;

	@FXML
	private javafx.scene.layout.HBox guestContainer;

	@FXML
	private javafx.scene.layout.HBox authContainer;

	@FXML
	private javafx.scene.control.Hyperlink usernameLink;

	@FXML
	private void initialize() {
		refreshThemeModeIcon();
		refreshAuthState();
	}

	private void refreshAuthState() {
		if (guestContainer == null || authContainer == null) return;
		boolean isAuthenticated = com.auction.client.service.UserSessionService.getInstance().isAuthenticated();
		
		guestContainer.setVisible(!isAuthenticated);
		guestContainer.setManaged(!isAuthenticated);
		
		authContainer.setVisible(isAuthenticated);
		authContainer.setManaged(isAuthenticated);
		
		if (isAuthenticated && usernameLink != null) {
			String displayName = com.auction.client.service.UserSessionService.getInstance().getCurrentUser().getFullName();
			if (displayName == null || displayName.isBlank()) {
				displayName = "Profile";
			}
			usernameLink.setText(displayName);
		}
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
	private void handleGoToProfile() {
		NavigationService.getInstance().navigateTo(SceneRegistry.PROFILE_PAGE);
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