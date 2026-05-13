//package com.jpb.reconciliation.reconciliation.exception;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.validation.FieldError;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//import com.jpb.reconciliation.reconciliation.dto.KalSuperUserResponseDto;
//import java.util.stream.Collectors;
//
//@RestControllerAdvice
//public class KalUserGlobalException {
//
//    @ExceptionHandler(KalUserCustomException.class)
//    public ResponseEntity<KalSuperUserResponseDto> handleCustomException(
//            KalUserCustomException ex) {
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(new KalSuperUserResponseDto(false, ex.getMessage(), null));
//    }
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<KalSuperUserResponseDto> handleValidationException(
//            MethodArgumentNotValidException ex) {
//
//        String errors = ex.getBindingResult()
//                .getFieldErrors()
//                .stream()
//                .map(FieldError::getDefaultMessage)
//                .collect(Collectors.joining("; "));
//
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(new KalSuperUserResponseDto(false, errors, null));
//    }
//}