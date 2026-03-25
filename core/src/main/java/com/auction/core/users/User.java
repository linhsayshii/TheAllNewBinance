package com.auction.core.users;

import com.auction.core.Entity;

public class User extends Entity {
    protected Integer id;
    protected String username;
    protected String password;
    protected String fullName;
    protected String email;
    protected Double balance;
    public enum Role { STANDARD, ADMIN }
    protected Role role;
    protected Boolean isActive;

    public User(Integer id, String username, String password, String fullName, String email, Double balance, Role role, Boolean isActive) {
        super(); // Lấy thời gian tạo
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.balance = balance != null ? balance : 0.0;
        this.role = role != null ? role : Role.STANDARD;
        this.isActive = isActive != null ? isActive : true;
    }

    public Integer getId() { return this.id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getUsername() { return this.username; }
    public void setUsername(String username) { this.username = username; updateTimestamp(); }
    
    public String getPassword() { return this.password; }
    public void setPassword(String password) { this.password = password; updateTimestamp(); }
    
    public String getFullName() { return this.fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; updateTimestamp(); }

    public String getEmail() { return this.email; }
    public void setEmail(String email) { this.email = email; updateTimestamp(); }

    public Double getBalance() { return this.balance; }
    public void setBalance(Double balance) { this.balance = balance; updateTimestamp();}
    
    public Role getRole() { return this.role; }
    public void setRole(Role role) { this.role = role; updateTimestamp(); }

    public Boolean getIsActive() { return this.isActive; }
    public void setIsActive(Boolean active) { this.isActive = active; updateTimestamp(); }
}