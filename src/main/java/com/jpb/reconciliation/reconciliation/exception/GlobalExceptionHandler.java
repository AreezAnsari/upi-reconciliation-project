package com.jpb.reconciliation.reconciliation.exception;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.jpb.reconciliation.reconciliation.dto.ErrorResponseDto;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	public ResponseEntity<ErrorResponseDto> handleGlobalException(Exception exception, WebRequest webRequests) {
		ErrorResponseDto errorResponseDto = new ErrorResponseDto(webRequests.getDescription(false),
				HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), LocalDate.now());

		return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(ResourceNotFoundException exception,
			WebRequest webRequest) {

		ErrorResponseDto errorResponseDto = new ErrorResponseDto(webRequest.getDescription(false), HttpStatus.NOT_FOUND,
				exception.getMessage(), LocalDate.now());

		return new ResponseEntity<>(errorResponseDto, HttpStatus.NOT_FOUND);
	}
	
	@ExceptionHandler(ResourceAlreadyFoundException.class)
	public ResponseEntity<ErrorResponseDto> handleResourceFoundException(ResourceAlreadyFoundException exception, WebRequest webRequest){
		
		ErrorResponseDto errorResponseDto = new ErrorResponseDto(webRequest.getDescription(false), 
				HttpStatus.FOUND, exception.getMessage(), 
				LocalDate.now());
		return new ResponseEntity<>(errorResponseDto, HttpStatus.FOUND);
	}
	
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ResponseDto> exceptionHandler(BadCredentialsException e) {
		ResponseDto responseDto = new ResponseDto("400", "Invalid Username or Password !");
		return new ResponseEntity<>(responseDto, HttpStatus.BAD_REQUEST);
	}
	
	@ExceptionHandler(DisabledException.class)
	public ResponseEntity<ResponseDto> exceptionUserHandler(BadCredentialsException e) {
		ResponseDto responseDto = new ResponseDto("400", "User is not approved.");
		return new ResponseEntity<>(responseDto, HttpStatus.BAD_REQUEST);
	}

}
