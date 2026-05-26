package com.auction.core.dto.wallet;

import java.math.BigDecimal;

/** Request DTO for depositing funds into a user's wallet. */
public class DepositRequest {
    private int userId;
    private BigDecimal amount;

    public DepositRequest() {}

    public DepositRequest(int userId, BigDecimal amount) {
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
