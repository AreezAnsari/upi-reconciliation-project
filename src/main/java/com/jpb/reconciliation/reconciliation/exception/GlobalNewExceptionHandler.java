package com.jpb.reconciliation.reconciliation.exception;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

@RestControllerAdvice
public class GlobalNewExceptionHandler {

 /**
  * 404 – Resource not found
  * Response:
  * {
  *   "status"    : "FAILURE",
  *   "statusMsg" : "Institution not found with ID: 99",
  *   "data"      : []
  * }
  */
 @ExceptionHandler(ResourceNotFoundException.class)
 public ResponseEntity<RestWithStatusList> handleNotFound(ResourceNotFoundException ex) {
     return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
 }

 /**
  * 409 – Duplicate resource
  * Response:
  * {
  *   "status"    : "FAILURE",
  *   "statusMsg" : "Institution with name 'X' already exists",
  *   "data"      : []
  * }
  */
 @ExceptionHandler(DuplicateResourceException.class)
 public ResponseEntity<RestWithStatusList> handleDuplicate(DuplicateResourceException ex) {
     return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
 }

 /**
  * 400 – Validation errors
  * Response:
  * {
  *   "status"    : "FAILURE",
  *   "statusMsg" : "Validation failed",
  *   "data"      : [ "institutionName: must not be blank", "emailAddress: invalid email" ]
  * }
  */
 @ExceptionHandler(MethodArgumentNotValidException.class)
 public ResponseEntity<RestWithStatusList> handleValidation(MethodArgumentNotValidException ex) {
     List<Object> errors = ex.getBindingResult()
             .getFieldErrors()
             .stream()
             .map(e -> (Object) (e.getField() + ": " + e.getDefaultMessage()))
             .collect(Collectors.toList());

     RestWithStatusList response = RestWithStatusList.builder()
             .status("FAILURE")
             .statusMsg("Validation failed")
             .data(errors)
             .build();

     return ResponseEntity.badRequest().body(response);
 }

 /**
  * 500 – Generic unexpected error
  * Response:
  * {
  *   "status"    : "FAILURE",
  *   "statusMsg" : "An unexpected error occurred",
  *   "data"      : []
  * }
  */
 @ExceptionHandler(Exception.class)
 public ResponseEntity<RestWithStatusList> handleGeneric(Exception ex) {
     return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
 }

 // ─── Helper ───────────────────────────────────────────────────────────────

 private ResponseEntity<RestWithStatusList> buildResponse(HttpStatus status, String message) {
     RestWithStatusList response = RestWithStatusList.builder()
             .status("FAILURE")
             .statusMsg(message)
             .data(Collections.emptyList())
             .build();
     return ResponseEntity.status(status).body(response);
 }
}