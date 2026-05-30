package com.auction.core.exception.wallet;

import com.auction.core.exception.DomainException;
import com.auction.core.exception.ErrorCode;

/**
 * Sealed base for all wallet and payment domain violations. Separated from the user identity
 * context per DDD Bounded Context principles.
 */
public abstract sealed class WalletException extends DomainException
        permits InsufficientBalanceException, InvalidAmountException, WalletTransactionException {

    protected WalletException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected WalletException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
