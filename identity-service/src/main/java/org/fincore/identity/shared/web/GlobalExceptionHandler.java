package org.fincore.identity.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import org.fincore.identity.auth.application.exception.AccountDisabledException;
import org.fincore.identity.auth.application.exception.AccountLockedException;
import org.fincore.identity.auth.application.exception.AccountPendingException;
import org.fincore.identity.auth.application.exception.InvalidCredentialsException;
import org.fincore.identity.shared.error.ApiError;
import org.fincore.identity.shared.error.ErrorCode;
import org.fincore.identity.user.application.exception.DuplicateUsernameException;
import org.fincore.identity.user.application.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ApiError> handleDuplicateUsername(
            DuplicateUsernameException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.CONFLICT,
                ErrorCode.DUPLICATE_USERNAME,
                exception.getMessage(),
                request
        );
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)

    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        String message = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField()
                                + ": "
                                + error.getDefaultMessage()
                )
                .collect(Collectors.joining(", "));

        return buildError(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                message,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(
            Exception exception,
            HttpServletRequest request
    ) {

        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred",
                request
        );
    }

    private ResponseEntity<ApiError> buildError(
            HttpStatus status,
            ErrorCode code,
            String message,
            HttpServletRequest request
    ) {

        String correlationId =
                request.getHeader(
                        CorrelationIdFilter.HEADER_NAME
                );

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                code,
                message,
                request.getRequestURI(),
                correlationId
        );

        return ResponseEntity
                .status(status)
                .body(error);
    }
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(
            UserNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.NOT_FOUND,
                ErrorCode.USER_NOT_FOUND,
                exception.getMessage(),
                request
        );
    }
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                ErrorCode.INVALID_CREDENTIALS,
                exception.getMessage(),
                request
        );
    }
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiError> handleAccountLocked(
            AccountLockedException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.LOCKED,
                ErrorCode.ACCOUNT_LOCKED,
                exception.getMessage(),
                request
        );
    }
    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ApiError> handleAccountDisabled(
            AccountDisabledException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.FORBIDDEN,
                ErrorCode.ACCOUNT_DISABLED,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(AccountPendingException.class)
    public ResponseEntity<ApiError> handleAccountPending(
            AccountPendingException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.FORBIDDEN,
                ErrorCode.ACCOUNT_PENDING,
                exception.getMessage(),
                request
        );
    }
}