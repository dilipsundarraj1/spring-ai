package com.llm.errorhandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Error> handleValidationErrors(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + " - " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        var error = new Error(
                errorMessage,
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.toString()
        );

        log.error("Validation error : {} ", errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Error> handleAllExceptions(Exception ex) {
        var error = new Error(
                ex.getMessage(),
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.toString()
        );

        log.error("Unexpected error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }


    public record Error(String errorMessage, LocalDateTime timestamp, String status) {
    }
}


