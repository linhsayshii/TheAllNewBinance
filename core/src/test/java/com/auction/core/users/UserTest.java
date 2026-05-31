package com.auction.core.users;

import static org.assertj.core.api.Assertions.*;

import com.auction.core.exception.wallet.InsufficientBalanceException;
import com.auction.core.exception.wallet.InvalidAmountException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {
    private User user;

    @BeforeEach
    void setUp() {
        // Sử dụng Factory để tái nạp thực thể
        user = UserFactory.createNewStandard("test_user", "pass", "Test User", "test@test.com");
        user.syncFinancialState(new BigDecimal("1000.00"), new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("Should successfully hold balance and increase locked balance")
    void testHoldBalance_Success() {
        user.holdBalance(new BigDecimal("200.00"));

        assertThat(user.getBalance()).isEqualByComparingTo("800.00");
        assertThat(user.getLockedBalance()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("Should prevent Double Spend / Over-locking (InsufficientBalanceException)")
    void testHoldBalance_DoubleSpend_ShouldFail() {
        // Giao dịch 1: Đóng băng 600 (Hợp lệ, số dư khả dụng còn 400)
        user.holdBalance(new BigDecimal("600.00"));

        // Giao dịch 2: Cố gắng đóng băng thêm 500 (Vượt quá 400 khả dụng)
        assertThatThrownBy(() -> user.holdBalance(new BigDecimal("500.00")))
                .isInstanceOf(InsufficientBalanceException.class);

        // Đảm bảo trạng thái tài chính không bị corrupt sau failed transaction
        assertThat(user.getBalance()).isEqualByComparingTo("400.00");
        assertThat(user.getLockedBalance()).isEqualByComparingTo("600.00");
    }

    @Test
    @DisplayName("Refund Invariant: Cannot refund more than currently locked balance")
    void testRefundBalance_Invariant_ShouldFail() {
        // Có 200 đang bị đóng băng
        user.syncFinancialState(new BigDecimal("1000.00"), new BigDecimal("200.00"));

        // Cố gắng hoàn tiền 300
        assertThatIllegalStateException()
                .isThrownBy(() -> user.refundBalance(new BigDecimal("300.00")))
                .withMessageContaining("không đủ");

        // Trạng thái giữ nguyên
        assertThat(user.getLockedBalance()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("Should successfully commit bid and decrease locked balance")
    void testCommitBid_Success() {
        user.syncFinancialState(new BigDecimal("1000.00"), new BigDecimal("200.00"));

        user.commitBid(new BigDecimal("150.00"));

        assertThat(user.getLockedBalance()).isEqualByComparingTo("50.00");
        assertThat(user.getBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Should throw Exception if ID is overwritten (Write-Once Security)")
    void testSetId_WriteOnceViolation() {
        user.setId(1); // Lần 1 hợp lệ

        assertThatIllegalStateException()
                .isThrownBy(() -> user.setId(2)) // Lần 2 phải crash
                .withMessageContaining("không thể sửa đổi");
    }

    @Test
    @DisplayName("Should throw InvalidAmountException on negative deposit")
    void testDeposit_NegativeAmount() {
        assertThatThrownBy(() -> user.deposit(new BigDecimal("-100.00")))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    @DisplayName("Should throw InvalidAmountException on zero deposit")
    void testDeposit_ZeroAmount() {
        assertThatThrownBy(() -> user.deposit(BigDecimal.ZERO))
                .isInstanceOf(InvalidAmountException.class);
    }
}
