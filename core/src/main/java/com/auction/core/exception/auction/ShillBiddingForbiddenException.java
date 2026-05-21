package com.auction.core.exception.auction;

import com.auction.core.exception.ErrorCode;

/** Thrown when the auction owner attempts to place a bid on their own auction (shill bidding). */
public final class ShillBiddingForbiddenException extends AuctionException {

    public ShillBiddingForbiddenException() {
        super(ErrorCode.SHILL_BIDDING_FORBIDDEN);
    }

    public ShillBiddingForbiddenException(String message) {
        super(ErrorCode.SHILL_BIDDING_FORBIDDEN, message);
    }
}
