package com.alfy.budget.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Which transactions a list shows. Any field left null / false doesn't filter.
 */
public record TransactionFilter(
        boolean needsCategorized,
        boolean needsTransferred,
        UUID categoryId,
        LocalDate start,
        LocalDate end
) {

    public static final TransactionFilter ALL = new TransactionFilter(false, false, null, null, null);

}
