package com.eventledger.config;

import com.eventledger.domain.exception.AccountNotFoundException;
import com.eventledger.domain.exception.CurrencyMismatchException;
import com.eventledger.domain.exception.IdempotencyConflictException;
import com.eventledger.domain.exception.InsufficientBalanceException;
import com.eventledger.domain.exception.InvalidStateTransitionException;
import com.eventledger.domain.exception.LedgerInvariantViolationException;
import com.eventledger.domain.exception.LockAcquisitionException;
import com.eventledger.domain.exception.PayoutNotFoundException;
import com.eventledger.domain.exception.StaleOwnerException;
import com.eventledger.domain.exception.UnbalancedPostingException;
import com.eventledger.domain.exception.UnsupportedCurrencyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
    public ProblemDetail handleAccountNotFound(AccountNotFoundException ex, HttpServletRequest request) {
        log.warn("Account not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Account Not Found", "account-not-found", ex.getMessage(), request);
    }

    @ExceptionHandler(PayoutNotFoundException.class)
    public ProblemDetail handlePayoutNotFound(PayoutNotFoundException ex, HttpServletRequest request) {
        log.warn("Payout not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Payout Not Found", "payout-not-found", ex.getMessage(), request);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ProblemDetail handleIdempotencyConflict(IdempotencyConflictException ex, HttpServletRequest request) {
        log.warn("Idempotency conflict: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Idempotency Conflict", "idempotency-conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ProblemDetail handleInsufficientBalance(InsufficientBalanceException ex, HttpServletRequest request) {
        log.warn("Insufficient balance: {}", ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient Balance", "insufficient-balance", ex.getMessage(), request);
    }

    @ExceptionHandler(CurrencyMismatchException.class)
    public ProblemDetail handleCurrencyMismatch(CurrencyMismatchException ex, HttpServletRequest request) {
        log.warn("Currency mismatch: {}", ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Currency Mismatch", "currency-mismatch", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidStateTransitionException ex, HttpServletRequest request) {
        log.warn("Invalid state transition: {}", ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid State Transition", "invalid-state-transition", ex.getMessage(), request);
    }

    @ExceptionHandler(LedgerInvariantViolationException.class)
    public ProblemDetail handleLedgerInvariant(LedgerInvariantViolationException ex, HttpServletRequest request) {
        log.error("Ledger invariant violation: {}", ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Ledger Invariant Violation", "ledger-invariant-violation", ex.getMessage(), request);
    }

    @ExceptionHandler(LockAcquisitionException.class)
    public ProblemDetail handleLockAcquisition(LockAcquisitionException ex, HttpServletRequest request) {
        log.warn("Lock acquisition failed: {}", ex.getMessage());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Lock Acquisition Failed", "lock-acquisition-failed", ex.getMessage(), request);
    }

    @ExceptionHandler(UnsupportedCurrencyException.class)
    public ProblemDetail handleUnsupportedCurrency(UnsupportedCurrencyException ex, HttpServletRequest request) {
        log.warn("Unsupported currency: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Unsupported Currency", "unsupported-currency", ex.getMessage(), request);
    }

    @ExceptionHandler(UnbalancedPostingException.class)
    public ProblemDetail handleUnbalancedPosting(UnbalancedPostingException ex, HttpServletRequest request) {
        log.error("Unbalanced posting detected: {}", ex.getMessage());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unbalanced Posting", "unbalanced-posting", ex.getMessage(), request);
    }

    @ExceptionHandler(StaleOwnerException.class)
    public ProblemDetail handleStaleOwner(StaleOwnerException ex, HttpServletRequest request) {
        log.warn("Stale lock owner: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Stale Lock Owner", "stale-lock-owner", ex.getMessage(), request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        ProblemDetail detail = problem(HttpStatus.CONFLICT, "Concurrent Modification", "optimistic-lock-conflict",
                "Resource was modified by another request. Retry the operation.", request);
        detail.setProperty("retryable", true);
        return detail;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Data Integrity Violation", "data-integrity-violation",
                "Operation conflicts with existing data.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() != null ? error.getDefaultMessage() : "invalid"
                ))
                .toList();
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Validation Failed", "validation-failed", "Request validation failed", request);
        detail.setProperty("fieldErrors", fieldErrors);
        return detail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<Map<String, String>> violations = ex.getConstraintViolations().stream()
                .map(violation -> Map.of(
                        "field", violation.getPropertyPath().toString(),
                        "message", violation.getMessage()
                ))
                .toList();
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Constraint Violation", "constraint-violation", "Request constraint violation", request);
        detail.setProperty("violations", violations);
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Malformed Request", "malformed-request", "Request body is malformed or missing", request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "internal-error",
                "An unexpected error occurred. Contact support if the problem persists.", request);
    }

    private ProblemDetail problem(HttpStatus status, String title, String errorCode, String detail, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("urn:event-ledger:error:" + errorCode));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("errorCode", errorCode);
        problemDetail.setProperty("timestamp", Instant.now().toString());
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            problemDetail.setProperty("traceId", traceId);
        }
        return problemDetail;
    }
}
