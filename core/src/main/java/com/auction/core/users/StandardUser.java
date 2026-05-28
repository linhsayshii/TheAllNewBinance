package com.auction.core.users;

import java.math.BigDecimal;

/**
 * Đại diện cho người dùng thông thường tham gia mua bán và đấu giá sản phẩm. Một tài khoản kiêm
 * nhiệm cả hai vai trò Người mua (Bidder) và Người bán (Seller), được phân biệt hoàn toàn bằng hành
 * vi đa hình dựa trên trạng thái isActive.
 */
public final class StandardUser extends User {

    /** Constructor phạm vi package-private. Chỉ được gọi bởi UserFactory. */
    StandardUser(
            Integer id,
            String username,
            String password,
            String fullName,
            String email,
            BigDecimal balance,
            BigDecimal lockedBalance,
            Boolean isActive) {
        super(
                id,
                username,
                password,
                fullName,
                email,
                balance,
                lockedBalance,
                Role.STANDARD,
                isActive);
    }

    @Override
    public boolean canBid() {
        return isActive; // Chỉ tài khoản đang hoạt động mới được phép đặt giá thầu
    }

    @Override
    public boolean canSell() {
        return isActive; // Chỉ tài khoản đang hoạt động mới được phép đăng bán sản phẩm
    }
}
