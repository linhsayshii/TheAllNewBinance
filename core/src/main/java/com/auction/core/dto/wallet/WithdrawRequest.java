package com.auction.core.dto.wallet;

import java.math.BigDecimal;

/** Request DTO for withdrawing funds from a user's wallet. */
public class WithdrawRequest {
    private int userId;
    private BigDecimal amount;

    public WithdrawRequest() {}

    public WithdrawRequest(int userId, BigDecimal amount) {
        this.userId = userId;
        this.amount = amount;
    }

    public int getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
