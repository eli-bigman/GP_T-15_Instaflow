package com.insightflow.model.enums;

/**
 * Maps to the DB CHECK constraint values in feedback_submissions.category.
 * Use {@link #getValue()} when writing to the DB, and {@link #fromValue(String)}
 * when reading from the external API payload.
 */
public enum FeedbackCategory {

    DELIVERY_SPEED("Delivery Speed"),
    PRODUCT_QUALITY("Product Quality"),
    APP_EXPERIENCE("App Experience"),
    PACKAGING("Packaging"),
    OVERALL("Overall");

    private final String value;

    FeedbackCategory(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static FeedbackCategory fromValue(String value) {
        for (FeedbackCategory cat : values()) {
            if (cat.value.equalsIgnoreCase(value)) return cat;
        }
        throw new IllegalArgumentException("Unknown feedback category: " + value);
    }
}
