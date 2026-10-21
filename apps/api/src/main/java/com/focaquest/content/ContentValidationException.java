package com.focaquest.content;

import java.util.List;

public class ContentValidationException extends RuntimeException {

    private final List<String> errors;

    public ContentValidationException(List<String> errors) {
        super("Conteúdo inválido (" + errors.size() + " erro(s)):\n  - " + String.join("\n  - ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}
