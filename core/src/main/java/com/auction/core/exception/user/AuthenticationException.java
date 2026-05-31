package com.auction.core.exception.user;

import com.auction.core.exception.ErrorCode;

/** Thrown when login credentials are invalid or authentication verification fails. */
public final class AuthenticationException extends UserException {

    public AuthenticationException() {
        super(ErrorCode.AUTHENTICATION_FAILED);
    }

    public AuthenticationException(String message) {
        super(ErrorCode.AUTHENTICATION_FAILED, message);
    }
}
