package com.auction.client.component.shell;

import com.auction.client.config.SceneRegistry;
import com.auction.client.scene.LifecycleAwareController;
import com.auction.client.scene.NavigationService;
import javafx.fxml.FXML;

public class NavbarComponentController implements LifecycleAwareController {

    @FXML
    private void handleGoToAuctions() {
        NavigationService.getInstance().navigateTo(SceneRegistry.GENERAL_PAGE);
    }

    @FXML
    private void handleGoToAllItems() {
        NavigationService.getInstance().navigateTo(SceneRegistry.GENERAL_PAGE);
    }

    @FXML
    private void handleGoToSellers() {
        NavigationService.getInstance().navigateTo(SceneRegistry.SELLERS_PAGE);
    }

    @Override
    public void onUnload() {
        // Implement socket handler cleanup here if Navbar registers any handlers later
        // e.g. NetworkService.getInstance().getClient().removeResponseHandler(...)
    }
}
