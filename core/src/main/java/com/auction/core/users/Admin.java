package com.auction.core.users;

public class Admin extends User {
    public Admin(String name, String password, String email) {
        super(name, password, email);
    }

    public void banUser(User badUser) {
        badUser.markAsDeleted();
        System.out.println("\n[Admin] Da khoa tai khoan nguoi dung: " + badUser.getName());
    }
}