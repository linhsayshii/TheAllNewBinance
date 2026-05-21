package com.auction.core.exception;

/**
 * Fallback domain exception for unrecognized or generic error codes received over the network.
 * Used exclusively on the client side when no specific subclass can be mapped.
 */
public final class GenericDomainException extends DomainException {

    public GenericDomainException(ErrorCode errorCode) {
        super(errorCode);
    }

    public GenericDomainException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
