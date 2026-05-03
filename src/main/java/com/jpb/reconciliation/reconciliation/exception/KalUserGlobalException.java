package com.jpb.reconciliation.reconciliation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.jpb.reconciliation.reconciliation.dto.KalApiResponseDto;

import java.util.stream.Collectors;

@RestControllerAdvice
public class KalUserGlobalException {

    /**
     * Handles custom business logic exceptions (username exists, user not found, etc.)
     */
    @ExceptionHandler(KalUserCustomException.class)
    public ResponseEntity<KalApiResponseDto> handleCustomException(KalUserCustomException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new KalApiResponseDto(false, ex.getMessage(), null));
    }

    /**
     * Handles @Valid annotation failures (pattern, email, notBlank, etc.)
     * Returns all field-level validation errors combined into one message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<KalApiResponseDto> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new KalApiResponseDto(false, errors, null));
    }
}
