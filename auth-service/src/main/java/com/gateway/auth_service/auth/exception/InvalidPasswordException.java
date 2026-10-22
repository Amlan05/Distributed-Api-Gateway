package com.gateway.auth_service.auth.exception;

public class InvalidPasswordException extends RuntimeException{

    public InvalidPasswordException(){
        super("Password must contain 12 to 72 characters "
              + "and must not exceed 72 UTF-8 bytes");
    }
}
