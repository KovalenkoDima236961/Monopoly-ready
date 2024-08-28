package com.dimon.catanbackend.exceptions;

public class EmailAlreadyInUseException extends RuntimeException{
    public EmailAlreadyInUseException(String message) {
        super(message);
    }
}
