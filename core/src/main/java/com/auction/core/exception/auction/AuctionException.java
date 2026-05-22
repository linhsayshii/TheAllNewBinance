package com.auction.core.exception.auction;

import com.auction.core.exception.DomainException;
import com.auction.core.exception.ErrorCode;

/**
 * Sealed base for all auction and bidding domain violations. Permits only known concrete auction
 * exception types for compiler-enforced exhaustiveness.
 */
public abstract sealed class AuctionException extends DomainException
        permits AuctionClosedException, InvalidBidException, ShillBiddingForbiddenException {

    protected AuctionException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected AuctionException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
