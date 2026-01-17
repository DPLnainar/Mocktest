package com.examportal.exception;

import com.examportal.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
                log.warn("Access denied: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.builder()
                                .error("FORBIDDEN")
                                .message(ex.getMessage())
                                .timestamp(LocalDateTime.now())
                                .path(request.getDescription(false).replace("uri=", ""))
                                .build();

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex,
                        WebRequest request) {
                String details = ex.getBindingResult().getFieldErrors().stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .collect(Collectors.joining(", "));

                log.warn("Validation failed: {}", details);

                ErrorResponse error = ErrorResponse.builder()
                                .error("VALIDATION_FAILED")
                                .message("Validation failed: " + details)
                                .timestamp(LocalDateTime.now())
                                .path(request.getDescription(false).replace("uri=", ""))
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                        WebRequest request) {
                log.warn("JSON parse error: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.builder()
                                .error("BAD_REQUEST")
                                .message("Malformed JSON request: " + ex.getMessage())
                                .timestamp(LocalDateTime.now())
                                .path(request.getDescription(false).replace("uri=", ""))
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                        WebRequest request) {
                log.warn("Type mismatch: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.builder()
                                .error("BAD_REQUEST")
                                .message(String.format("Parameter '%s' should be of type %s", ex.getName(),
                                                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName()
                                                                : "unknown"))
                                .timestamp(LocalDateTime.now())
                                .path(request.getDescription(false).replace("uri=", ""))
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(HttpMessageConversionException.class)
        public ResponseEntity<ErrorResponse> handleHttpMessageConversion(HttpMessageConversionException ex,
                        WebRequest request) {
                log.error("JSON conversion error: ", ex);

                ErrorResponse error = ErrorResponse.builder()
                                .error("BAD_REQUEST")
                                .message("Could not read JSON: " + ex.getMessage())
                                .timestamp(LocalDateTime.now())
                                .path(request.getDescription(false).replace("uri=", ""))
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, WebRequest request) {
                ErrorResponse error = ErrorResponse.builder()
                                .error("UNAUTHORIZED")
                                .message("Invalid username or password")
                                .timestamp(LocalDateTime.now())
                                .path(request.getDescription(false).replace("uri=", ""))
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {
                log.error("Runtime exception: ", ex);

                ErrorResponse error = ErrorResponse.builder()
                                .error("BAD_REQUEST")
                                .message(ex.getMessage())
                                .timestamp(LocalDateTime.now())
                                .path(request.getDescription(false).replace("uri=", ""))
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
                log.error("Unhandled exception", ex);

                ErrorResponse error = ErrorResponse.builder()
                                .error("INTERNAL_SERVER_ERROR")
                                .message("An unexpected error occurred: " + ex.getClass().getName() + " - "
                                                + ex.getMessage())
                                .timestamp(LocalDateTime.now())
                                .path(request.getDescription(false).replace("uri=", ""))
                                .build();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
}
