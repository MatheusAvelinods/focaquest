package com.focaquest.questions;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class QuestionOption {

    @Column(nullable = false, length = 1)
    private String label;
    @Column(nullable = false)
    private String text;
    @Column(nullable = false)
    private String rationale;

    protected QuestionOption() {
    }

    public QuestionOption(String label, String text, String rationale) {
        this.label = label;
        this.text = text;
        this.rationale = rationale;
    }

    public String getLabel() { return label; }
    public String getText() { return text; }
    public String getRationale() { return rationale; }
}
