package com.auction.core.exception;

/**
 * Abstract base class for all domain business exceptions in the auction system.
 *
 * <p>Design rationale:
 *
 * <ul>
 *   <li>Abstract: prevents direct instantiation; all concrete exceptions must belong to a
 *       recognized domain subdomain (auction, wallet, user).
 *   <li>Sub-hierarchies are sealed at their own package level (AuctionException, WalletException,
 *       UserException) enforcing exhaustiveness checks at the subdomain switch level.
 *   <li>Stackless: overrides fillInStackTrace() to prevent JVM stack capture, reducing network
 *       payload size and preventing server internals from leaking to the client.
 * </ul>
 */
public abstract class DomainException extends RuntimeException {

    private final ErrorCode errorCode;

    protected DomainException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    protected DomainException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Disables stack trace capture for performance and security in distributed socket transfers.
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
