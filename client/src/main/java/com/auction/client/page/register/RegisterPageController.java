package com.auction.client.page.register;

import com.auction.client.config.SceneRegistry;
import com.auction.client.scene.LifecycleAwareController;
import com.auction.client.scene.NavigationService;
import com.auction.client.service.NetworkService;
import com.auction.client.service.UserSessionService;
import com.auction.core.dto.user.RegisterRequest;
import com.auction.core.protocol.EventType;
import com.auction.core.users.User;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class RegisterPageController implements Initializable, LifecycleAwareController {

    private static final String HANDLER_ID = "REGISTER_PAGE";

    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;

    private final RegisterPageViewModel viewModel = new RegisterPageViewModel();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        NetworkService.getInstance().getClient()
            .addResponseHandler(EventType.REGISTER, HANDLER_ID, this::onRegisterResponse);
    }

    @FXML
    private void handleRegister() {
        String username = txtUsername.getText();
        String email    = txtEmail.getText();
        String password = txtPassword.getText();

        if (!viewModel.validateRegistration(username, email, password)) {
            lblError.setText("Vui lòng nhập đầy đủ thông tin hợp lệ.");
            lblError.setVisible(true);
            return;
        }

        lblError.setVisible(false);
        RegisterRequest req = new RegisterRequest(username, password, username, email);
        NetworkService.getInstance().sendRequest(EventType.REGISTER, req);
    }

    @FXML
    private void handleGoToLogin() {
        NavigationService.getInstance().navigateTo(SceneRegistry.LOGIN_PAGE);
    }

    private void onRegisterResponse(String rawJson) {
        User user = viewModel.parseRegisterResponse(rawJson);
        Platform.runLater(() -> {
            if (user != null) {
                UserSessionService.getInstance().login(user);
                NavigationService.getInstance().navigateTo(SceneRegistry.GENERAL_PAGE);
            } else {
                lblError.setText(viewModel.parseErrorMessage(rawJson));
                lblError.setVisible(true);
            }
        });
    }

    @Override
    public void onUnload() {
        NetworkService.getInstance().getClient()
            .removeResponseHandler(EventType.REGISTER, HANDLER_ID);
    }
}