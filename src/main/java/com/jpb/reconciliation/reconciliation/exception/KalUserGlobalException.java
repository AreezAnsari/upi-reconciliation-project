package com.jpb.reconciliation.reconciliation.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class KalUserGlobalException {

    @ExceptionHandler(KalUserCustomException.class)
    public ResponseEntity<?> handleCustom(KalUserCustomException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}