package com.auction.core.users;

import java.math.BigDecimal;

/** Đại diện cho quản trị viên vận hành và kiểm duyệt hệ thống đấu giá. */
public final class Admin extends User {

    /** Constructor phạm vi package-private. Chỉ được gọi bởi UserFactory. */
    Admin(
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
                Role.ADMIN,
                isActive);
    }

    @Override
    public boolean canManageSystem() {
        return isActive; // Chỉ Admin đang hoạt động mới có quyền truy cập hệ thống quản trị
    }
}
