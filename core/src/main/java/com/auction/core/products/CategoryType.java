package com.auction.core.products;

/**
 * Định nghĩa 8 danh mục sản phẩm chính thức được hỗ trợ bởi hệ thống sàn đấu giá. Sử dụng thuộc
 * tính displayName tiếng Anh để hiển thị trực tiếp lên UI (FXML/Label), loại bỏ hoàn toàn việc map
 * thủ công rườm rà ở client.
 */
public enum CategoryType {
    WATCHES("Watches"),
    FASHION("Fashion"),
    ART("Art"),
    MUSIC("Music"),
    COLLECTIBLES("Collectibles"),
    SPORTS("Sports"),
    CAMERAS("Cameras"),
    WINE("Wine");

    private final String displayName;

    CategoryType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
