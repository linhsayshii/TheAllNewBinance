package com.auction.core.dto.auction;

/**
 * Request DTO cho tính năng Promote Listing lên Star Auction.
 * packageDays: 1 hoặc 3 (ngày)
 * shortDescription: Mô tả ngắn gọn. Nếu null/blank, server sẽ tự fallback về 100 ký tự đầu của Item Description.
 * sellerId: Được Server tự override từ Session (client không cần gửi).
 * adminForce: Nếu true → Admin promote trực tiếp, bỏ qua kiểm tra balance.
 */
public class PromoteAuctionRequest {
    private Integer auctionId;
    private Integer packageDays;   // 1 hoặc 3
    private String shortDescription;
    private Integer sellerId;      // override bởi server từ session
    private Boolean adminForce;    // chỉ admin dùng được

    public PromoteAuctionRequest() {}

    public Integer getAuctionId() { return auctionId; }
    public void setAuctionId(Integer auctionId) { this.auctionId = auctionId; }

    public Integer getPackageDays() { return packageDays; }
    public void setPackageDays(Integer packageDays) { this.packageDays = packageDays; }

    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }

    public Integer getSellerId() { return sellerId; }
    public void setSellerId(Integer sellerId) { this.sellerId = sellerId; }

    public Boolean getAdminForce() { return adminForce != null && adminForce; }
    public void setAdminForce(Boolean adminForce) { this.adminForce = adminForce; }
}
