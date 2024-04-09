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
        return new BigDecimal(integer).divide(ONE_HUNDRED, RoundingMode.CEILING);
    }

    public static boolean isLessThanZero(BigDecimal bigDecimal) {
        return bigDecimal == null || bigDecimal.compareTo(BigDecimal.ZERO) < 0;
    }

}
