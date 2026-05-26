package com.auction.core.exception.user;

import com.auction.core.exception.ErrorCode;

/** Thrown when a requested user ID cannot be located in the system. */
public final class UserNotFoundException extends UserException {

    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }

    public UserNotFoundException(String message) {
        super(ErrorCode.USER_NOT_FOUND, message);
    }
}
