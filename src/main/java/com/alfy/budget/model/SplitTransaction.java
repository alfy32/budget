package com.alfy.budget.model;

import java.util.UUID;

public class SplitTransaction {

    public UUID id;
    public int index;
    public String description;
    public UUID categoryId;
    public double amount;

}
