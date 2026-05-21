package com.auction.client.component.shell;

import org.kordamp.ikonli.javafx.FontIcon;

import com.auction.client.config.SceneRegistry;
import com.auction.client.scene.NavigationService;

import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import com.auction.core.users.User;

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
	private javafx.scene.control.Button adminBtn;

	private ChangeListener<User> authChangeListener;

	@FXML
	private void initialize() {
		setupUserMenu();
		refreshThemeModeIcon();
		refreshAuthState();
		
		authChangeListener = (obs, oldVal, newVal) -> refreshAuthState();
		com.auction.client.service.UserSessionService.getInstance().currentUserProperty().addListener(new WeakChangeListener<>(authChangeListener));
	}

	private void setupUserMenu() {
		if (usernameLink == null) return;
		
		ContextMenu userMenu = new ContextMenu();
		userMenu.getStyleClass().add("user-context-menu");
		
		MenuItem profileItem = new MenuItem("Profile");
		profileItem.setOnAction(e -> handleGoToProfile());
		
		MenuItem logoutItem = new MenuItem("Log out");
		logoutItem.setOnAction(e -> handleLogout());
		
		userMenu.getItems().addAll(profileItem, logoutItem);
		
		usernameLink.setOnAction(e -> {
			userMenu.show(usernameLink, javafx.geometry.Side.BOTTOM, 0, 0);
		});
	}

	private void refreshAuthState() {
		if (guestContainer == null || authContainer == null) return;
		boolean isAuthenticated = com.auction.client.service.UserSessionService.getInstance().isAuthenticated();
		
		guestContainer.setVisible(!isAuthenticated);
		guestContainer.setManaged(!isAuthenticated);
		
		authContainer.setVisible(isAuthenticated);
		authContainer.setManaged(isAuthenticated);
		
		if (isAuthenticated && usernameLink != null) {
			com.auction.core.users.User user = com.auction.client.service.UserSessionService.getInstance().getCurrentUser();
			String displayName = user.getFullName();
			if (displayName == null || displayName.isBlank()) {
				displayName = "Profile";
			}
			usernameLink.setText(displayName);

			// Admin button — show only for ADMIN role (UX convenience; server validates anyway)
			boolean isAdmin = user.getRole() == com.auction.core.users.User.Role.ADMIN;
			if (adminBtn != null) {
				adminBtn.setVisible(isAdmin);
				adminBtn.setManaged(isAdmin);
			}
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
		com.auction.core.users.User user = com.auction.client.service.UserSessionService.getInstance().getCurrentUser();
		if (user != null && user.getId() != null) {
			NavigationService.getInstance().navigateTo(SceneRegistry.PROFILE_PAGE, 
				java.util.Map.of("userId", user.getId()));
		} else {
			NavigationService.getInstance().navigateTo(SceneRegistry.PROFILE_PAGE);
		}
	}

	@FXML
	private void handleGoToAdmin() {
		NavigationService.getInstance().navigateTo(SceneRegistry.ADMIN_PAGE);
	}

	@FXML
	private void handleGoToRegister() {
		NavigationService.getInstance().openPopup(SceneRegistry.REGISTER_CARD);
	}

	@FXML
	private void handleLogout() {
		com.auction.client.service.UserSessionService.getInstance().logout();
		refreshAuthState();
		NavigationService.getInstance().navigateTo(SceneRegistry.GENERAL_PAGE);
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