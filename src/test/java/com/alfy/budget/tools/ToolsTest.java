package com.alfy.budget.tools;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ToolsTest {

    @Test
    public void test() {
        testBigDecimalConversion("230.18", 23018);
        testBigDecimalConversion("74.71", 7471);
        testBigDecimalConversion("64.07", 6407);
        testBigDecimalConversion("38.66", 3866);
        testBigDecimalConversion("624.17", 62417);
        testBigDecimalConversion("4.56", 456);
    }

    @Test
    public void percentAsInt() {
        assertEquals(50, Tools.percentAsInt(new BigDecimal("1"), new BigDecimal("2")));
        assertEquals(33, Tools.percentAsInt(new BigDecimal("1"), new BigDecimal("3")));
    }

    private static void testBigDecimalConversion(String stringValue, int integerValue) {
        int integer = Tools.toDatabaseInt(new BigDecimal(stringValue));
        assertEquals(integerValue, integer);
        BigDecimal bigDecimal = Tools.fromDatabaseInt(integer);
        assertEquals(new BigDecimal(stringValue), bigDecimal);
    }
}