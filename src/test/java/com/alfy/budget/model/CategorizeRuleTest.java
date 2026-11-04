package com.alfy.budget.model;

import com.alfy.budget.model.CategorizeRule.MatchType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CategorizeRuleTest {

    private static CategorizeRule rule(String text, MatchType type) {
        CategorizeRule rule = new CategorizeRule();
        rule.description = text;
        rule.matchType = type;
        return rule;
    }

    @Test
    void matchTypesIgnoreCase() {
        String amazon = "AMAZON MKTPL*047279EJ3   Amzn.com/billWA";

        assertTrue(rule("amazon mktpl", MatchType.CONTAINS).matches(amazon));
        assertTrue(rule("Amzn.com", MatchType.CONTAINS).matches(amazon));
        assertTrue(rule("AMAZON", MatchType.STARTS_WITH).matches(amazon));
        assertFalse(rule("MKTPL", MatchType.STARTS_WITH).matches(amazon));
        assertFalse(rule("AMAZON MKTPL", MatchType.EQUALS).matches(amazon));
        assertTrue(rule("venmo/payment", MatchType.EQUALS).matches("VENMO/PAYMENT"));
        assertTrue(rule("VENMO/PAYMENT", MatchType.EQUALS).matches("  VENMO/PAYMENT "));
        assertFalse(rule("", MatchType.CONTAINS).matches(amazon));
        assertFalse(rule("WALMART", MatchType.CONTAINS).matches(null));
    }

    @Test
    void mostSpecificRuleComesFirst() {
        List<CategorizeRule> rules = new ArrayList<>(List.of(
                rule("WALMART", MatchType.CONTAINS),
                rule("POS PUR-WALMART", MatchType.STARTS_WITH),
                rule("WALMART.COM", MatchType.CONTAINS),
                rule("WALMART.COM 800-925-6278 AR", MatchType.EQUALS)
        ));
        rules.sort(CategorizeRule.MOST_SPECIFIC_FIRST);

        assertEquals(List.of("WALMART.COM 800-925-6278 AR", "POS PUR-WALMART", "WALMART.COM", "WALMART"),
                rules.stream().map(rule -> rule.description).toList());
    }

}
