package com.gateway.user.exception;

public class ProfileAlreadyExistsException extends RuntimeException {
    public ProfileAlreadyExistsException() {
        super("This account already has a user profile");
    }
}
