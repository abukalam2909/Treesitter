package ca.dal.treefactor.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ExceptionHandlingController extends ResponseEntityExceptionHandler {
	
	@ExceptionHandler(value = { IllegalArgumentException.class })
	protected ResponseEntity<Object> illegalArgument(RuntimeException e) {
		return ResponseEntity.badRequest().body(e.getLocalizedMessage());
	}
}
