package com.auction.core.exception.wallet;

import com.auction.core.exception.ErrorCode;

/** Thrown when a deposit or withdraw amount is null, zero, or negative. */
public final class InvalidAmountException extends WalletException {

    public InvalidAmountException() {
        super(ErrorCode.INVALID_TRANSACTION_AMOUNT);
    }

    public InvalidAmountException(String message) {
        super(ErrorCode.INVALID_TRANSACTION_AMOUNT, message);
    }
}
