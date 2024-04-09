package com.alfy.budget.model;

import java.math.BigDecimal;
import java.util.UUID;

public class SplitTransaction {

    public UUID id;
    public int index;
    public String description;
    public UUID categoryId;
    public BigDecimal amount;

}
