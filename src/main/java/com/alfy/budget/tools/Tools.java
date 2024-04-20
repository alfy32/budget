package com.alfy.budget.tools;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Tools {

    public static final BigDecimal ONE_HUNDRED = new BigDecimal(100);

    public static int toDatabaseInt(BigDecimal bigDecimal) {
        if (bigDecimal == null) {
            return 0;
        }

        return bigDecimal.multiply(ONE_HUNDRED).intValue();
    }

    public static BigDecimal fromDatabaseInt(int integer) {
        return new BigDecimal(integer).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }

    public static boolean isLessThanZero(BigDecimal bigDecimal) {
        return bigDecimal == null || bigDecimal.compareTo(BigDecimal.ZERO) < 0;
    }

    public static int percentAsInt(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null) {
            return 0;
        }

        if (BigDecimal.ZERO.compareTo(denominator) == 0) {
            return 0;
        }

        return ONE_HUNDRED.multiply(numerator).divide(denominator, 0, RoundingMode.HALF_UP).intValue();
    }

}
