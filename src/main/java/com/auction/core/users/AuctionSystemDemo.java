package com.auction.core.users;

import com.auction.core.products.KhoToiUu;

public class AuctionSystemDemo {
    public static void main(String[] args) {
        KhoToiUu kho = new KhoToiUu();
        Admin admin = new Admin("Tran Quan Tri", "pass", "admin@sys.com");
        StandardUser seller = new StandardUser("Nguyen Cong Minh", "pass", "ban@gmail.com");
        StandardUser bidder1 = new StandardUser("Hoang Viet Ling", "pass", "mua1@gmail.com");
        StandardUser bidder2 = new StandardUser("Khanh", "pass", "mua2@gmail.com");

        System.out.println("KHOI DONG HE THONG...\n");

        // 1. Đăng bán sản phẩm
        seller.addProduct("Binh gom su", 1000.0, kho);
        seller.addProduct("Tranh son dau", 500.0, kho);
        
        kho.hienThiKho();

        // Lấy danh sách ID thực tế đang có trong kho để test
        Object[] idList = kho.danhSachSanPham.keySet().toArray();
        String idBinhGom = (String) idList[0];
        String idTranh = (String) idList[1];

        // 2. Đấu giá Bình gốm
        bidder1.placeBid(idBinhGom, 800.0, kho);  // Sẽ thất bại vì thấp hơn giá gốc
        bidder1.placeBid(idBinhGom, 1200.0, kho); // Thành công
        bidder2.placeBid(idBinhGom, 1500.0, kho); // Thành công, vượt bidder1

        // 3. Người bán quyết định gỡ bức tranh xuống (Không bán nữa)
        seller.removeProduct(idTranh, kho);

        // 4. Ai đó cố tình đấu giá bức tranh đã gỡ
        bidder1.placeBid(idTranh, 1000.0, kho); // Sẽ báo lỗi không tồn tại

        // 5. Xem lại kho hàng (Bức tranh sẽ bị ẩn đi, chỉ còn Bình gốm)
        kho.hienThiKho();
        
        // 6. Admin xử lý vi phạm
        admin.banUser(bidder1);
    }
}