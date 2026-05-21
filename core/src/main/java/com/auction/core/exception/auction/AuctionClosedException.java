package com.auction.core.exception.auction;

import com.auction.core.exception.ErrorCode;

/** Thrown when a bid is placed on an auction that has already ended or is not yet active. */
public final class AuctionClosedException extends AuctionException {

    public AuctionClosedException() {
        super(ErrorCode.AUCTION_CLOSED);
    }

    public AuctionClosedException(String message) {
        super(ErrorCode.AUCTION_CLOSED, message);
    }
}
