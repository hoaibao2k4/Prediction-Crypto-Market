package com.market.prediction.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.market.prediction.dto.response.ErrorResponse;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex,
            HttpServletRequest httpRequest) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), httpRequest);
    }

    @ExceptionHandler(ConflictResourceException.class)
    public ResponseEntity<ErrorResponse> handleConflictResourceException(ConflictResourceException ex,
            HttpServletRequest httpRequest) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), httpRequest);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex,
            HttpServletRequest httpRequest) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), httpRequest);
    }

    @ExceptionHandler(BinanceProviderException.class)
    public ResponseEntity<ErrorResponse> handleBinanceProviderException(BinanceProviderException ex,
            HttpServletRequest httpRequest) {
        log.error("Binance Provider Error: ", ex);
        return buildErrorResponse(HttpStatus.BAD_GATEWAY, "Error fetching data from Binance. Please try again later.", httpRequest);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex,
            HttpServletRequest httpRequest) {
        return buildErrorResponse(ex.getStatus(), ex.getMessage(), httpRequest);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex,
            HttpServletRequest httpRequest) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildErrorResponse(HttpStatus.BAD_REQUEST, errorMessage, httpRequest);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex,
            HttpServletRequest httpRequest) {
        String message = "Malformed JSON request or data type mismatch";
        if (ex.getMessage() != null && ex.getMessage().contains("Floating-point value")) {
            message = "Data type mismatch: Integer value expected, but floating-point found.";
        }
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, httpRequest);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex,
            HttpServletRequest httpRequest) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized access: Please log in to continue.", httpRequest);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex,
            HttpServletRequest httpRequest) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Access denied: You do not have permission.", httpRequest);
    }

    @ExceptionHandler({
            ExpiredJwtException.class,
            SignatureException.class,
            MalformedJwtException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleJwtException(Exception ex, HttpServletRequest httpServletRequest) {
        String message = "Invalid JWT Token";
        if (ex instanceof ExpiredJwtException) {
            message = "Token has expired, please log in again";
        } else if (ex instanceof SignatureException) {
            message = "Invalid token signature";
        } else if (ex instanceof MalformedJwtException) {
            message = "Invalid token format";
        }
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, message, httpServletRequest);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex,
            HttpServletRequest httpServletRequest) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password", httpServletRequest);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex,
            HttpServletRequest httpServletRequest) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Data integrity violation: some fields might be duplicated.",
                httpServletRequest);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException ex,
            HttpServletRequest httpRequest) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "The requested path was not found.", httpRequest);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailureException(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Data has been modified by another user. Please refresh and try again.",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex,
            HttpServletRequest httpRequest) {
        log.error("Unhandled exception occurred at {}: ", httpRequest.getRequestURI(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please contact support.",
                httpRequest);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message,
            HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .error(status.name())
                .message(message)
                .build();
        return ResponseEntity.status(status).body(errorResponse);
    }
}
