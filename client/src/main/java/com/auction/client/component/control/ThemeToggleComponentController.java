package com.auction.client.component.control;

import com.auction.client.service.ThemeService;

import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;

public class ThemeToggleComponentController {

    @FXML
    private ToggleButton themeToggle;

    private final ThemeService themeService = new ThemeService();

    @FXML
    private void onThemeToggle() {
        if (themeToggle != null && themeToggle.getScene() != null) {
            themeService.toggle(themeToggle.getScene());
        }
    }
}