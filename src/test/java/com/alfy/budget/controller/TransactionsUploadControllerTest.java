package com.alfy.budget.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionsUploadControllerTest {

    @Test
    public void test() {
        assertEquals("GUNNISON MARKET", TransactionsUploadController.cleanZionsDescription("GUNNISON MARKET GUNNISON UT (GUNNISON MARKET)"));
        assertEquals("SQ *LITTLE CAESARS OF GUN", TransactionsUploadController.cleanZionsDescription("SQ *LITTLE CAESARS OF GUN Gunnison UT (SQ *LITTLE CAESARS OF GUN)"));
    }

}