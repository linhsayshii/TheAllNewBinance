package com.auction.core.users;

import com.auction.core.dto.user.UserDto;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Factory chịu trách nhiệm tái nạp (Rehydration) và khởi tạo thực thể User.
 * Do nằm cùng package 'com.auction.core.users', Factory có đặc quyền truy cập
 * constructor package-private của các lớp con để đảm bảo tính đóng gói đối với bên ngoài.
 */
public final class UserFactory {

    private UserFactory() {
        // Ngăn chặn khởi tạo instance
    }

    /**
     * Tái lập thực thể User từ Database (Rehydration).
     * Sử dụng Switch Expression của Java 21 với Exhaustiveness check để đảm bảo an toàn mở rộng.
     *
     * @param roleStr       Vai trò dưới dạng chuỗi ký tự từ Database ("STANDARD", "ADMIN")
     * @param id            Mã định danh duy nhất
     * @param username      Tên đăng nhập
     * @param password      Mật khẩu đã băm
     * @param fullName      Tên đầy đủ
     * @param email         Địa chỉ email
     * @param balance       Số dư khả dụng (BigDecimal)
     * @param lockedBalance Số dư đóng băng (BigDecimal)
     * @param isActive      Trạng thái tài khoản
     * @return Đối tượng User đa hình cụ thể (StandardUser hoặc Admin)
     * @throws IllegalArgumentException Nếu chuỗi roleStr không hợp lệ
     */
    public static User rehydrateUser(
            String roleStr,
            Integer id,
            String username,
            String password,
            String fullName,
            String email,
            BigDecimal balance,
            BigDecimal lockedBalance,
            Boolean isActive) {
        Objects.requireNonNull(roleStr, "Role string không được null");

        return switch (roleStr.trim().toUpperCase()) {
            case "STANDARD" -> new StandardUser(
                    id, username, password, fullName, email, balance, lockedBalance, isActive);
            case "ADMIN" -> new Admin(
                    id, username, password, fullName, email, balance, lockedBalance, isActive);
            default -> throw new IllegalArgumentException("Vai trò không hợp lệ: " + roleStr);
        };
    }

    /**
     * Tạo mới một Standard User (Dành cho chức năng đăng ký tài khoản).
     *
     * @param username       Tên đăng nhập
     * @param hashedPassword Mật khẩu đã băm từ tầng Service
     * @param fullName       Tên đầy đủ
     * @param email          Địa chỉ email
     * @return Một thực thể StandardUser mới với số dư bằng 0
     */
    public static User createNewStandard(
            String username, String hashedPassword, String fullName, String email) {
        return new StandardUser(
                null,
                username,
                hashedPassword,
                fullName,
                email,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true);
    }

    /**
     * Chuyển đổi thực thể Domain User sang DTO an toàn (UserDto) để truyền qua Socket.
     * Kích hoạt Dynamic Dispatch tại Server Node để nhúng kết quả đa hình vào DTO phẳng,
     * triệt tiêu hoàn toàn logic phân rã chuỗi Role cứng ở phía Client JavaFX.
     *
     * @param user Thực thể Domain User ở phía Server
     * @return Đối tượng UserDto an toàn để gửi về phía Client (không chứa password)
     */
    public static UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getBalance(),
                user.getLockedBalance(),
                user.getIsActive(),
                user.canBid(),           // Thực thi đa hình tại Server Node
                user.canSell(),          // Thực thi đa hình tại Server Node
                user.canManageSystem()); // Thực thi đa hình tại Server Node
    }
}
