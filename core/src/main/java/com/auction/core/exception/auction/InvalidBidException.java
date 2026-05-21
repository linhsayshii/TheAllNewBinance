package com.auction.core.exception.auction;

import com.auction.core.exception.ErrorCode;

/** Thrown when a bid amount is zero, negative, non-finite, or below the minimum bid step. */
public final class InvalidBidException extends AuctionException {

    public InvalidBidException() {
        super(ErrorCode.INVALID_BID_AMOUNT);
    }

    public InvalidBidException(String message) {
        super(ErrorCode.INVALID_BID_AMOUNT, message);
    }
}
