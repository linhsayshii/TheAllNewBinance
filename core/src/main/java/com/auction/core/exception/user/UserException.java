package com.auction.core.exception.user;

import com.auction.core.exception.DomainException;
import com.auction.core.exception.ErrorCode;

/**
 * Sealed base for all identity and access domain violations.
 * Governs user authentication and access control failures.
 */
public sealed abstract class UserException extends DomainException
        permits AuthenticationException {

    protected UserException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected UserException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
