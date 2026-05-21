package com.auction.client.component.profile;

import com.auction.client.page.profile.ProfilePageViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Controller for the shared profile sidebar user-card component.
 *
 * <p>Usage from parent controller:
 *
 * <pre>
 *   {@literal @}FXML private ProfileSidebarController profileSidebarController;
 *   // then call:
 *   profileSidebarController.bind(viewModel);
 * </pre>
 *
 * The fx:id on the {@code <fx:include>} must be {@code "profileSidebar"} so that JavaFX injects the
 * nested controller as {@code profileSidebarController}.
 */
public class ProfileSidebarController {

    @FXML private StackPane avatarCircle;
    @FXML private Label avatarInitialLabel;
    @FXML private Label usernameLabel;
    @FXML private Label userIdTagLabel;
    @FXML private Label joinDateLabel;
    @FXML private Label emailLabel;

    /**
     * Binds all sidebar labels to the given ViewModel's observable properties. Must be called after
     * the parent FXML has finished loading.
     */
    public void bind(ProfilePageViewModel vm) {
        avatarInitialLabel.textProperty().bind(vm.avatarInitialProperty());
        usernameLabel.textProperty().bind(vm.displayNameProperty());
        userIdTagLabel.textProperty().bind(vm.userIdTagProperty());
        joinDateLabel.textProperty().bind(vm.joinDateProperty());
        emailLabel.textProperty().bind(vm.emailProperty());
    }
}
