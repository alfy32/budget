package com.alfy.budget.model;

import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;

public class CategorizeRule {

    public enum MatchType {
        EQUALS("equals", "Is exactly"),
        STARTS_WITH("starts_with", "Starts with"),
        CONTAINS("contains", "Contains");

        public final String dbValue;
        public final String label;

        MatchType(String dbValue, String label) {
            this.dbValue = dbValue;
            this.label = label;
        }

        public static MatchType fromDb(String value) {
            for (MatchType type : values()) {
                if (type.dbValue.equals(value)) {
                    return type;
                }
            }
            return EQUALS;
        }
    }

    /**
     * When several rules match, the first one in this order wins: exact beats starts-with beats contains,
     * then the longer (more specific) text wins.
     */
    public static final Comparator<CategorizeRule> MOST_SPECIFIC_FIRST = Comparator
            .comparing((CategorizeRule rule) -> rule.matchType.ordinal())
            .thenComparing(rule -> -rule.description.length())
            .thenComparing(rule -> rule.description);

    public UUID id;
    public String description;
    public MatchType matchType = MatchType.CONTAINS;
    public UUID categoryId;
    public String categoryName;

    /** Ignores upper/lower case and spaces at the ends. */
    public boolean matches(String bankDescription) {
        if (bankDescription == null || description == null || description.isBlank()) {
            return false;
        }

        String text = bankDescription.strip().toLowerCase(Locale.ROOT);
        String rule = description.strip().toLowerCase(Locale.ROOT);
        return switch (matchType) {
            case EQUALS -> text.equals(rule);
            case STARTS_WITH -> text.startsWith(rule);
            case CONTAINS -> text.contains(rule);
        };
    }

}
