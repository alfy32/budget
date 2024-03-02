package com.alfy.budget.model;

import java.util.UUID;

public class Category {

    public UUID id;
    public String name;

    public static Category create(String name) {
        Category category = new Category();
        category.id = UUID.randomUUID();
        category.name = name;
        return category;
    }

}
