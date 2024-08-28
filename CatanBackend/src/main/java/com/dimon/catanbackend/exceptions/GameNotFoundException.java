package com.dimon.catanbackend.exceptions;

public class GameNotFoundException extends RuntimeException{
    public GameNotFoundException(String message) {
        super(message);
    }
}
