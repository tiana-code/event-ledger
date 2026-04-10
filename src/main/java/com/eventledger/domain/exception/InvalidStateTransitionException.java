package com.eventledger.domain.exception;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String currentState, String targetState) {
        super("Invalid state transition from %s to %s".formatted(currentState, targetState));
    }
}
