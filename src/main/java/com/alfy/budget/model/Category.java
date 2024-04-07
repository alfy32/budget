package com.alfy.budget.model;

import java.util.UUID;

public class Category {

    public UUID id;
    public String name;
    public Budget budget;

    public static Category create(String name) {
        return create(name, null);
    }

    public static Category create(String name, Budget budget) {
        Category category = new Category();
        category.id = UUID.randomUUID();
        category.name = name;
        category.budget = budget;
        return category;
    }

}
