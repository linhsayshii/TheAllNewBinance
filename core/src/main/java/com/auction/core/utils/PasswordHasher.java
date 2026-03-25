package com.auction.core.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {
    private static final int DEFAULT_COST = 12;
    private PasswordHasher() {}
    public static String hash(String plainPassword) {
        if (plainPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(DEFAULT_COST));
    }
}
