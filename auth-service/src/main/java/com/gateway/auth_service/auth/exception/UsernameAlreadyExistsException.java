package com.gateway.auth_service.auth.exception;

public class UsernameAlreadyExistsException extends RuntimeException{

    public UsernameAlreadyExistsException(){
        super("Username is already registered");
    }
}
