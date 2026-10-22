package com.gateway.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 254)
    private String email;

    // Nullable only for profiles created before account ownership was introduced.
    @Column(name = "owner_subject", unique = true, length = 100, updatable = false)
    private String ownerSubject;

    public String getOwnerSubject() {
        return ownerSubject;
    }

    protected UserEntity() {
    }


    public UserEntity(String name, String email) {
        this(name, email, null);
    }

    public UserEntity(String name, String email, String ownerSubject) {
        this.name = name;
        this.email = email;
        this.ownerSubject = ownerSubject;
    }

    public Long getId() {
        return id;
    }

    public String getName(){
        return name;
    }

    public String getEmail(){
        return email;
    }

    public void updateDetails(String name, String email) {
        this.name = name;
        this.email = email;
    }


}
