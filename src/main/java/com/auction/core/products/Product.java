package com.auction.core.products;

import com.auction.core.Entity;
import java.util.UUID;

public class Product extends Entity {
    private String id;
    private String name;
    private double currentPrice;
    private String highestBidderName;

    public Product(String name, double startingPrice) {
        super(); // Lấy thời gian đăng sản phẩm
        this.id = UUID.randomUUID().toString(); // Tự động sinh ID duy nhất cho mỗi sp
        this.name = name;
        this.currentPrice = startingPrice;
        this.highestBidderName = "Chua co ai";
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getCurrentPrice() { return currentPrice; }

    public boolean isValidBid(double newBidAmount, String bidderName) {  // Hàm bid 
        if (newBidAmount > this.currentPrice) {
            this.currentPrice = newBidAmount;
            this.highestBidderName = bidderName;
            this.updateTimestamp(); // Cập nhật thời gian có biến động giá mới nhất
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("[ID: %s] %s | Gia: $%.2f | Nguoi dan dau: %s | Dang luc: %s",
                id.substring(0, 8) + "...", // Viết tắt UUID cho dễ nhìn
                name, currentPrice, highestBidderName, getFormattedCreatedAt());
    }
}
