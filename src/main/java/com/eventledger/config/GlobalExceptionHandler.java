package com.eventledger.config;

import com.eventledger.domain.exception.AccountNotFoundException;
import com.eventledger.domain.exception.CurrencyMismatchException;
import com.eventledger.domain.exception.IdempotencyConflictException;
import com.eventledger.domain.exception.InsufficientBalanceException;
import com.eventledger.domain.exception.InvalidStateTransitionException;
import com.eventledger.domain.exception.LedgerInvariantViolationException;
import com.eventledger.domain.exception.LockAcquisitionException;
import com.eventledger.domain.exception.NegativeBalanceException;
import com.eventledger.domain.exception.PayoutNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFound(AccountNotFoundException ex) {
        log.warn("Account not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Account Not Found", "account-not-found", ex.getMessage());
    }

    @ExceptionHandler(PayoutNotFoundException.class)
    public ProblemDetail handlePayoutNotFound(PayoutNotFoundException ex) {
        log.warn("Payout not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Payout Not Found", "payout-not-found", ex.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ProblemDetail handleIdempotencyConflict(IdempotencyConflictException ex) {
        log.warn("Idempotency conflict: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Idempotency Conflict", "idempotency-conflict", ex.getMessage());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ProblemDetail handleInsufficientBalance(InsufficientBalanceException ex) {
        log.warn("Insufficient balance: {}", ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient Balance", "insufficient-balance", ex.getMessage());
    }

    @ExceptionHandler(NegativeBalanceException.class)
    public ProblemDetail handleNegativeBalance(NegativeBalanceException ex) {
        log.warn("Negative balance rejected: {}", ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Negative Balance", "negative-balance", ex.getMessage());
    }

    @ExceptionHandler(CurrencyMismatchException.class)
    public ProblemDetail handleCurrencyMismatch(CurrencyMismatchException ex) {
        log.warn("Currency mismatch: {}", ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Currency Mismatch", "currency-mismatch", ex.getMessage());
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidStateTransitionException ex) {
        log.warn("Invalid state transition: {}", ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid State Transition", "invalid-state-transition", ex.getMessage());
    }

    @ExceptionHandler(LedgerInvariantViolationException.class)
    public ProblemDetail handleLedgerInvariant(LedgerInvariantViolationException ex) {
        log.error("Ledger invariant violation: {}", ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Ledger Invariant Violation", "ledger-invariant-violation", ex.getMessage());
    }

    @ExceptionHandler(LockAcquisitionException.class)
    public ProblemDetail handleLockAcquisition(LockAcquisitionException ex) {
        log.warn("Lock acquisition failed: {}", ex.getMessage());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Lock Acquisition Failed", "lock-acquisition-failed", ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Concurrent Modification", "optimistic-lock-conflict",
                "Resource was modified by another request. Retry the operation.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Data Integrity Violation", "data-integrity-violation",
                "Operation conflicts with existing data.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() != null ? error.getDefaultMessage() : "invalid"
                ))
                .toList();
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Validation Failed", "validation-failed", "Request validation failed");
        detail.setProperty("fieldErrors", fieldErrors);
        return detail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        List<Map<String, String>> violations = ex.getConstraintViolations().stream()
                .map(violation -> Map.of(
                        "field", violation.getPropertyPath().toString(),
                        "message", violation.getMessage()
                ))
                .toList();
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Constraint Violation", "constraint-violation", "Request constraint violation");
        detail.setProperty("violations", violations);
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Malformed Request", "malformed-request", "Request body is malformed or missing");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "internal-error",
                "An unexpected error occurred. Contact support if the problem persists.");
    }

    private ProblemDetail problem(HttpStatus status, String title, String errorCode, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("urn:event-ledger:error:" + errorCode));
        problemDetail.setProperty("errorCode", errorCode);
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }
}
