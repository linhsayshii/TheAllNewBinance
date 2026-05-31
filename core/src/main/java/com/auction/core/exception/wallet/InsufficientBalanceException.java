package com.auction.core.exception.wallet;

import com.auction.core.exception.ErrorCode;

/** Thrown when a user's account balance is insufficient to hold the 30% auction deposit. */
public final class InsufficientBalanceException extends WalletException {

    public InsufficientBalanceException() {
        super(ErrorCode.INSUFFICIENT_BALANCE);
    }

    public InsufficientBalanceException(String message) {
        super(ErrorCode.INSUFFICIENT_BALANCE, message);
    }
}
