package com.alfy.budget.controller;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Applies to every form and query parameter: trims text and turns empty text into null,
 * so "?query=" and a blank input mean the same as leaving it out.
 */
@ControllerAdvice
public class FormDefaults {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

}
