package com.auction.core.exception.wallet;

import com.auction.core.exception.ErrorCode;

/** Thrown when a DB write for a wallet operation (balance update, ledger insert) fails. */
public final class WalletTransactionException extends WalletException {

    public WalletTransactionException() {
        super(ErrorCode.WALLET_TRANSACTION_FAILED);
    }

    public WalletTransactionException(String message) {
        super(ErrorCode.WALLET_TRANSACTION_FAILED, message);
    }
}
