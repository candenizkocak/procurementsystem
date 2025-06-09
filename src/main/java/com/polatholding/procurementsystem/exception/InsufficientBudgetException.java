package com.polatholding.procurementsystem.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST) // This will result in a 400 Bad Request error if uncaught
public class InsufficientBudgetException extends RuntimeException {
    public InsufficientBudgetException(String message) {
        super(message);
    }
}