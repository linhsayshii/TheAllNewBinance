package com.auction.client.page.login;

import com.auction.client.config.SceneRegistry;
import com.auction.client.scene.LifecycleAwareController;
import com.auction.client.scene.NavigationService;
import com.auction.client.service.NetworkService;
import com.auction.client.service.UserSessionService;
import com.auction.client.service.notification.NotificationService;
import com.auction.client.service.notification.NotificationType;
import com.auction.core.dto.user.LoginRequest;
import com.auction.core.protocol.EventType;
import com.auction.core.users.User;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginPageController implements Initializable, LifecycleAwareController {

    private static final String HANDLER_ID = "LOGIN_CARD";

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;

    private final LoginPageViewModel viewModel = new LoginPageViewModel();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        NetworkService.getInstance()
                .getClient()
                .addResponseHandler(EventType.LOGIN, HANDLER_ID, this::onLoginResponse);
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (!viewModel.validateCredentials(username, password)) {
            lblError.setText("Please enter valid credentials.");
            lblError.setVisible(true);
            NotificationService.getInstance()
                    .show("Please enter valid credentials.", NotificationType.WARNING);
            return;
        }

        lblError.setVisible(false);
        NetworkService.getInstance()
                .sendRequest(EventType.LOGIN, new LoginRequest(username, password));
    }

    @FXML
    private void handleGoToRegister() {
        if (NavigationService.getInstance().isPopupOpen()) {
            NavigationService.getInstance().openPopup(SceneRegistry.REGISTER_CARD);
            return;
        }
        NavigationService.getInstance().navigateTo(SceneRegistry.REGISTER_CARD);
    }

    private void onLoginResponse(String rawJson) {
        User user = viewModel.parseLoginResponse(rawJson);
        Platform.runLater(
                () -> {
                    if (user != null) {
                        UserSessionService.getInstance().login(user);
                        NotificationService.getInstance()
                                .show(
                                        "Logged in successfully! Welcome back, "
                                                + user.getUsername()
                                                + ".",
                                        NotificationType.SUCCESS);
                        if (NavigationService.getInstance().isPopupOpen()) {
                            NavigationService.getInstance().closePopup();
                            return;
                        }
                        NavigationService.getInstance().navigateTo(SceneRegistry.GENERAL_PAGE);
                    } else {
                        String errMsg = viewModel.parseErrorMessage(rawJson);
                        lblError.setText(errMsg);
                        lblError.setVisible(true);
                        NotificationService.getInstance().show(errMsg, NotificationType.ERROR);
                    }
                });
    }

    @Override
    public void onUnload() {
        NetworkService.getInstance().getClient().removeResponseHandler(EventType.LOGIN, HANDLER_ID);
    }
}
