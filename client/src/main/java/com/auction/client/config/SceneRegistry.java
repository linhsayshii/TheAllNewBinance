package com.auction.client.config;

public enum SceneRegistry {
    GENERAL_PAGE("/fxml/pages/general-page.fxml", "General"),
    LOGIN_CARD("/fxml/components/auth/login-card.fxml", "Login"),
    REGISTER_CARD("/fxml/components/auth/register-card.fxml", "Register"),
    AUCTION_PAGE("/fxml/pages/auction-page.fxml", "Auction Detail"),
    PROFILE_PAGE("/fxml/pages/personal-profile-page.fxml", "My Profile"),
    PUBLIC_SELLER_PAGE("/fxml/pages/public-seller-page.fxml", "Seller Profile"),
    CREATE_LISTING_PAGE("/fxml/pages/create-listing-page.fxml", "Create Listing"),
    SELLERS_PAGE("/fxml/pages/sellers-page.fxml", "Sellers"),
    ADMIN_PAGE("/fxml/pages/admin-page.fxml", "Admin Portal"),
    CATEGORIZED_AUCTION_PAGE("/fxml/pages/categorized-auction-page.fxml", "Explore Auctions");

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
