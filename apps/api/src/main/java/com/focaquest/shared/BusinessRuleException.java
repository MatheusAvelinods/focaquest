package com.focaquest.shared;

/** Requisição bem formada que viola uma regra de negócio. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
