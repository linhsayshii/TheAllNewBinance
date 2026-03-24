package com.auction.core.users;

import com.auction.core.products.KhoToiUu;
import com.auction.core.products.Product;

public class StandardUser extends User implements IBidder, ISeller {
    public StandardUser(String name, String password, String email) {
        super(name, password, email);
    }

    @Override
    public void addProduct(String productName, double startingPrice, KhoToiUu kho) {
        Product newProduct = new Product(productName, startingPrice);
        kho.themSanPham(newProduct);
        System.out.println("[Nguoi ban] " + this.name + " da dang ban: '" + productName + "'");
        System.out.println("   -> ID cap phat: " + newProduct.getId());
    }

    @Override
    public void removeProduct(String productId, KhoToiUu kho) {
        Product p = kho.timSanPham(productId);
        if (p != null) {
            p.markAsDeleted(); // Dùng xóa mềm thay vì xóa hẳn khỏi HashMap
            System.out.println("\n[Nguoi Ban] " + this.name + " da go san pham: " + p.getName());
        }
    }

    @Override
    public void placeBid(String productId, double amount, KhoToiUu kho) {
        System.out.println("[Nguoi mua] " + this.name + " dat gia $" + amount + "...");
        
        Product p = kho.timSanPham(productId);
        
        if (p == null) {
            System.out.println("   -> LOI: San pham khong ton tai hoac da bi go !!!");
            return;
        }
        
        if (p.isValidBid(amount, this.name)) {
            System.out.println("   -> THANH CONG: Vuon len dan dau !!!");
        } else {
            System.out.println("   -> THAT BAI: Muc gia qua thap so voi hien tai ($" + p.getCurrentPrice() + ")");
        }
    }
}