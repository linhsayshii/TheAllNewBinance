package com.auction.client.component.shell;

import com.auction.client.config.SceneRegistry;
import com.auction.client.scene.NavigationService;
import com.auction.client.scene.LifecycleAwareController;
import javafx.fxml.FXML;

public class NavbarComponentController implements LifecycleAwareController {

    @FXML
    private void handleGoToGeneral() {
        NavigationService.getInstance().navigateTo(SceneRegistry.GENERAL_PAGE);
    }

    @FXML
    private void handleGoToLogin() {
        NavigationService.getInstance().navigateTo(SceneRegistry.LOGIN_PAGE);
    }

    @FXML
    private void handleGoToRegister() {
        NavigationService.getInstance().navigateTo(SceneRegistry.REGISTER_PAGE);
    }

    @FXML
    private void handleGoToProductDetail() {
        NavigationService.getInstance().navigateTo(SceneRegistry.PRODUCT_DETAIL_PAGE);
    }

    @FXML
    private void handleGoToProfile() {
        NavigationService.getInstance().navigateTo(SceneRegistry.PROFILE_PAGE);
    }

    @Override
    public void onUnload() {
        // Implement socket handler cleanup here if Navbar registers any handlers later
        // e.g. NetworkService.getInstance().getClient().removeResponseHandler(...)
    }
}