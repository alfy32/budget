package com.alfy.budget.controller;

import com.webcohesion.ofx4j.client.FinancialInstitution;
import com.webcohesion.ofx4j.client.FinancialInstitutionData;
import com.webcohesion.ofx4j.client.impl.BaseFinancialInstitutionData;
import com.webcohesion.ofx4j.io.BaseOFXReader;
import com.webcohesion.ofx4j.io.nanoxml.NanoXMLOFXReader;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping(path = "/hello")
    public String sayHello() {
        return "Hello World";
    }
}
