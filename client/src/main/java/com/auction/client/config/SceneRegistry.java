package com.auction.client.config;

public enum SceneRegistry {
    GENERAL_PAGE("/fxml/pages/general-page.fxml", "General"),
    LOGIN_CARD("/fxml/components/auth/login-card.fxml", "Login"),
    REGISTER_CARD("/fxml/components/auth/register-card.fxml", "Register"),
    AUCTION_PAGE("/fxml/pages/auction-page.fxml", "Auction Detail"),
    PROFILE_PAGE("/fxml/pages/profile-page.fxml", "Profile"),
    SELLERS_PAGE("/fxml/pages/sellers-page.fxml", "Sellers");

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