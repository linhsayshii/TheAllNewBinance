package com.auction.core.users;

public class Admin extends User {
    public Admin(
            Integer id,
            String username,
            String password,
            String fullName,
            String email,
            Double balance) {
        super(id, username, password, fullName, email, balance, Role.ADMIN, true);
    }
}
