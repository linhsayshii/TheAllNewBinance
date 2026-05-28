package com.auction.core.exception.auction;

import com.auction.core.exception.ErrorCode;

/**
 * Thrown when the scheduler fails to transition an auction from PENDING to ACTIVE, either because
 * the auction is not in PENDING status or the DB update failed.
 */
public final class AuctionActivationException extends AuctionException {

    public AuctionActivationException() {
        super(ErrorCode.AUCTION_ACTIVATION_FAILED);
    }

    public AuctionActivationException(String message) {
        super(ErrorCode.AUCTION_ACTIVATION_FAILED, message);
    }
}
