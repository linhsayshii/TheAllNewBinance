package com.auction.core.exception;

/**
 * Categorized numeric error codes for the distributed auction system.
 *
 * <p>Code ranges follow enterprise layering convention:
 *
 * <ul>
 *   <li>1xxx - Infrastructure and network layer errors
 *   <li>2xxx - Identity and access context (user authentication)
 *   <li>25xx - Wallet and payment context (financial operations)
 *   <li>3xxx - Auction and bidding domain invariant violations
 * </ul>
 */
public enum ErrorCode {

    // 1xxx: Infrastructure & Network Context
    MALFORMED_REQUEST(1001, "Invalid request packet format."),
    NETWORK_TIMEOUT(1002, "Server connection timed out."),

    // 2xxx: Identity & Access Context
    AUTHENTICATION_FAILED(2003, "Authentication failed. Invalid credentials."),
    USER_NOT_FOUND(2004, "User could not be found."),

    // 25xx: Wallet & Payment Context
    INSUFFICIENT_BALANCE(2501, "Account balance is insufficient to hold the 30% deposit."),
    INVALID_TRANSACTION_AMOUNT(2502, "The transaction amount is invalid or non-positive."),
    WALLET_TRANSACTION_FAILED(2503, "The wallet transaction operation failed."),

    // 3xxx: Bidding & Auction Context
    AUCTION_CLOSED(3003, "The auction has ended or is not active."),
    SHILL_BIDDING_FORBIDDEN(3004, "Sellers are forbidden from bidding on their own auctions."),
    INVALID_BID_AMOUNT(3005, "Bid amount is invalid or below current highest bid."),
    AUCTION_ACTIVATION_FAILED(3006, "Failed to activate the auction from PENDING to ACTIVE."),
    AUCTION_SETTLEMENT_FAILED(3007, "Failed to settle the auction, it must be in ACTIVE status.");

    private final int value;
    private final String defaultMessage;

    ErrorCode(int value, String defaultMessage) {
        this.value = value;
        this.defaultMessage = defaultMessage;
    }

    public int getValue() {
        return value;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    /**
     * Resolves an ErrorCode from its integer value. Falls back to MALFORMED_REQUEST if the value is
     * unrecognized.
     */
    public static ErrorCode fromValue(int value) {
        for (ErrorCode code : values()) {
            if (code.getValue() == value) {
                return code;
            }
        }
        return MALFORMED_REQUEST;
    }
}
