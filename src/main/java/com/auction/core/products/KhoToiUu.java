package com.auction.core.products;

import java.util.HashMap;
import java.util.Map;

public class KhoToiUu {
    public Map<String, Product> danhSachSanPham; // Để public tạm thời để hàm main dễ lấy ID test

    public KhoToiUu() {
        this.danhSachSanPham = new HashMap<>();
    }

    public void themSanPham(Product p) {
        danhSachSanPham.put(p.getId(), p);
    }

    public Product timSanPham(String productId) {
        Product p = danhSachSanPham.get(productId);
        // Chỉ trả về sản phẩm nếu nó tồn tại và chưa bị xóa
        if (p != null && !p.isDeleted()) {
            return p;
        }
        return null; 
    }

    public void hienThiKho() {
        System.out.println("\n=== BANG SAN PHAM DANG DAU GIA ===");
        boolean coSanPham = false;
        for (Product p : danhSachSanPham.values()) {
            if (!p.isDeleted()) { // Bỏ qua các sản phẩm đã bị xóa
                System.out.println(p);
                coSanPham = true;
            }
        }
        if (!coSanPham) System.out.println("Hien khong co san pham nao tren san dau gia.");
        System.out.println("===================================\n");
    }
}