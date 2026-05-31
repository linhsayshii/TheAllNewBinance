package com.auction.client.network;

import com.auction.core.exception.DomainException;
import com.auction.core.exception.ErrorCode;
import com.auction.core.exception.GenericDomainException;
import com.auction.core.exception.auction.AuctionClosedException;
import com.auction.core.exception.auction.InvalidBidException;
import com.auction.core.exception.auction.ShillBiddingForbiddenException;
import com.auction.core.exception.user.AuthenticationException;
import com.auction.core.exception.wallet.InsufficientBalanceException;

/**
 * Factory that converts a numeric error code received over the WebSocket wire into a typed
 * DomainException on the client side. Enables type-safe, exhaustive handling in UI controllers
 * using Java 21 pattern matching switch without brittle string comparisons.
 */
public final class ClientExceptionFactory {

    private ClientExceptionFactory() {}

    /**
     * Creates the most specific DomainException subclass for the given numeric error code.
     *
     * @param errorCodeValue the integer value transmitted in the JSON errorCode field
     * @param message the human-readable message received from the server
     * @return a typed DomainException ready for pattern-matching dispatch in the UI layer
     */
    public static DomainException create(int errorCodeValue, String message) {
        ErrorCode errorCode = ErrorCode.fromValue(errorCodeValue);
        return switch (errorCode) {
            case AUCTION_CLOSED -> new AuctionClosedException(message);
            case INVALID_BID_AMOUNT -> new InvalidBidException(message);
            case SHILL_BIDDING_FORBIDDEN -> new ShillBiddingForbiddenException(message);
            case INSUFFICIENT_BALANCE -> new InsufficientBalanceException(message);
            case AUTHENTICATION_FAILED -> new AuthenticationException(message);
            default -> new GenericDomainException(errorCode, message);
        };
    }
}
