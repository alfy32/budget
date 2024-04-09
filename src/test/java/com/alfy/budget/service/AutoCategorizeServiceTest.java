package com.alfy.budget.service;

import com.alfy.budget.model.BankTransaction;
import com.alfy.budget.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AutoCategorizeServiceTest {

    AutoCategorizeService autoCategorizeService;

    Category investmentsCategory = Category.create("Investments");
    Category paycheckCategory = Category.create("Paycheck");

    @Mock
    private CategoriesService categoriesService;
    @Mock
    private TransactionsService transactionsService;

    @BeforeEach
    public void beforeEach() {
        autoCategorizeService = new AutoCategorizeService(categoriesService, transactionsService);

        when(categoriesService.list()).thenReturn(Arrays.asList(
                investmentsCategory,
                paycheckCategory
        ));

        autoCategorizeService.updateCachedData();

        verify(categoriesService, times(1)).list();
    }

    @Test
    public void noCategory() {
        UUID transactionId = UUID.randomUUID();

        BankTransaction bankTransaction = new BankTransaction();
        bankTransaction.description = "Random one that will not categorize";
        boolean categorized = autoCategorizeService.autoCategorize(
                bankTransaction, transactionId
        );

        assertFalse(categorized);

        verifyNoMoreInteractions(
                categoriesService,
                transactionsService
        );
    }

    @Test
    public void vangaurd() {
        UUID transactionId = UUID.randomUUID();

        BankTransaction bankTransaction = new BankTransaction();
        bankTransaction.description = "VANGUARD BUY/INVESTMENT";
        boolean categorized = autoCategorizeService.autoCategorize(
                bankTransaction, transactionId
        );

        assertTrue(categorized);

        verify(transactionsService, times(1)).updateCategory(transactionId, investmentsCategory.id);

        verifyNoMoreInteractions(
                categoriesService,
                transactionsService
        );
    }

    @Test
    public void paycheck() {
        UUID transactionId = UUID.randomUUID();

        BankTransaction bankTransaction = new BankTransaction();
        bankTransaction.description = "Check Payroll/PAY";
        boolean categorized = autoCategorizeService.autoCategorize(
                bankTransaction, transactionId
        );

        assertTrue(categorized);

        verify(transactionsService, times(1)).updateCategory(transactionId, paycheckCategory.id);

        verifyNoMoreInteractions(
                categoriesService,
                transactionsService
        );
    }
}