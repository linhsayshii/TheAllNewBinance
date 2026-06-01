package com.auction.core.users;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserFactoryTest {

    @Test
    @DisplayName("Should rehydrate StandardUser when role is STANDARD")
    void testRehydrateUser_StandardRole() {
        User user =
                UserFactory.rehydrateUser(
                        "STANDARD",
                        1,
                        "test",
                        "pass",
                        "Full",
                        "email",
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        true);

        assertThat(user).isInstanceOf(StandardUser.class);
        assertThat(user.getUsername()).isEqualTo("test");
    }

    @Test
    @DisplayName("Should rehydrate Admin when role is ADMIN")
    void testRehydrateUser_AdminRole() {
        User user =
                UserFactory.rehydrateUser(
                        "ADMIN",
                        2,
                        "admin",
                        "pass",
                        "Full",
                        "email",
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        true);

        assertThat(user).isInstanceOf(Admin.class);
    }

    @Test
    @DisplayName("Should throw exception for unknown user role")
    void testRehydrateUser_UnknownRole() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                UserFactory.rehydrateUser(
                                        "UNKNOWN_ROLE",
                                        3,
                                        "test",
                                        "pass",
                                        "Full",
                                        "email",
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO,
                                        true))
                .withMessageContaining("Vai trò không hợp lệ");
    }
}
