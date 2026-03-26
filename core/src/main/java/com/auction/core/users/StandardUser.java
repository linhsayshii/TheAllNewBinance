package com.auction.core.users;

public class StandardUser extends User {
    public StandardUser(Integer id, String username, String password, String fullName, String email, Double balance) {
        super(id, username, password, fullName, email, balance, Role.STANDARD, true);
    }
}