package com.auction.core.dto.user;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Data Transfer Object (DTO) dạng phẳng, bất biến (Java 21 Immutable Record). Cách ly tuyệt đối
 * trường password và logic phân quyền cứng khỏi phía Client. Triển khai Serializable để truyền tải
 * an toàn qua Java Socket.
 */
// spotless:off
public record UserDto(
        Integer id,
        String username,
        String fullName,
        String email,
        String role,
        BigDecimal balance,
        BigDecimal lockedBalance,
        boolean isActive,
        boolean canBid, // Kết quả đa hình được thực thi tại Server Node
        boolean canSell, // Kết quả đa hình được thực thi tại Server Node
        boolean canManageSystem // Kết quả đa hình được thực thi tại Server Node
) implements Serializable {
// spotless:on

    private static final long serialVersionUID = 1L;

    /** Compact Constructor thực hiện kiểm tra tính toàn vẹn dữ liệu. */
    public UserDto {
        Objects.requireNonNull(username, "Username không được null");
        Objects.requireNonNull(role, "Role không được null");
        balance = balance != null ? balance : BigDecimal.ZERO;
        lockedBalance = lockedBalance != null ? lockedBalance : BigDecimal.ZERO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserDto userDto = (UserDto) o;
        return Objects.equals(id, userDto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
