package com.auction.core.users;

import com.auction.core.Entity;

import java.util.UUID;
public class User extends Entity {
    protected final String id;
    protected String name;
    protected String password;
    protected String email;

    public User(String name, String password, String email) {
        super(); // gọi constructor Entity để lấy tgian khởi tạo
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.password = password;
        this.email = email;
    }
    public String getId() { return this.id; }
    public String getName() { return this.name; }
}