package com.auction.client.config;

public enum SceneRegistry {
    GENERAL_PAGE("/fxml/pages/general-page.fxml", "General"),
    LOGIN_CARD("/fxml/components/auth/login-card.fxml", "Login"),
    REGISTER_CARD("/fxml/components/auth/register-card.fxml", "Register"),
    PRODUCT_DETAIL_PAGE("/fxml/pages/product-detail-page.fxml", "Product Detail"),
    PROFILE_PAGE("/fxml/pages/profile-page.fxml", "Profile");

    private final String fxmlPath;
    private final String title;

    SceneRegistry(String fxmlPath, String title) {
        this.fxmlPath = fxmlPath;
        this.title = title;
    }

    public String fxmlPath() {
        return fxmlPath;
    }

    public String title() {
        return title;
    }
}