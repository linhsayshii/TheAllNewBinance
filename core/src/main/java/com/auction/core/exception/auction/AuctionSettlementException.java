package com.auction.core.exception.auction;

import com.auction.core.exception.ErrorCode;

/**
 * Thrown when the scheduler fails to settle an auction that is not in ACTIVE status,
 * ensuring the settlement task never runs on auctions in incorrect states.
 */
public final class AuctionSettlementException extends AuctionException {

    public AuctionSettlementException() {
        super(ErrorCode.AUCTION_SETTLEMENT_FAILED);
    }

    public AuctionSettlementException(String message) {
        super(ErrorCode.AUCTION_SETTLEMENT_FAILED, message);
    }
}
