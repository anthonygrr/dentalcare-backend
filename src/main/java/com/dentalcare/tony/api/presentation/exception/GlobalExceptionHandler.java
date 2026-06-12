package com.dentalcare.tony.api.presentation.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    //  Validations (@Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<FieldErrorDetail> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(
                        err -> FieldErrorDetail
                                .builder()
                                .field(err.getField())
                                .error(err.getDefaultMessage())
                                .build())
                .toList();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Validation error")
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .errors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);

    }


    //    UNIQUE
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        String message = "Data Integrity Violation";
        if (ex.getMessage().contains("uk_patient_email")) {
            message = "The email already exists";
        } else if (ex.getMessage().contains("uk_patient_phone")) {
            message = "The phone already exists";
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(message)
                .status(HttpStatus.CONFLICT.value())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .errors(null)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }


    // GENERAL
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex,
            HttpServletRequest request
    ) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Server Internal Error")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .errors(null)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }


    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(
            UserNotFoundException ex
    ) {

        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.NOT_FOUND
                );

        problem.setTitle("User Not Found");
        problem.setDetail(ex.getMessage());

        return problem;
    }

    @ExceptionHandler(
            InvalidCredentialsException.class
    )
    public ProblemDetail handleInvalidCredentials(
            InvalidCredentialsException ex
    ) {

        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.UNAUTHORIZED
                );

        problem.setTitle("Invalid Credentials");
        problem.setDetail(ex.getMessage());

        return problem;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(
            BusinessException ex
    ) {

        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.BAD_REQUEST
                );

        problem.setTitle("Business Error");
        problem.setDetail(ex.getMessage());

        return problem;
    }

}
