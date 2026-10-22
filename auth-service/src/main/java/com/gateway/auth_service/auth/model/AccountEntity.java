package com.gateway.auth_service.auth.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled;

    protected AccountEntity(){

    }

    public AccountEntity(String username, String passwordHash){
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = Role.USER;
        this.enabled = true;
    }

    public static AccountEntity administrator(String username, String passwordHash) {
        AccountEntity account = new AccountEntity(username, passwordHash);
        account.role = Role.ADMIN;
        return account;
    }

    public Long getId(){
        return Id;
    }

    public String getUsername(){
        return username;
    }

    public String getPasswordHash(){
        return passwordHash;
    }

    public Role getRole(){
        return role;
    }

    public boolean isEnabled(){
        return enabled;
    }

}
