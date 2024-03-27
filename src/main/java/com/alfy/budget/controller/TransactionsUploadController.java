package com.alfy.budget.controller;

import com.alfy.budget.model.BankTransaction;
import com.alfy.budget.model.TransactionUploadResults;
import com.alfy.budget.service.BankTransactionsService;
import com.alfy.budget.service.TransactionsService;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping(path = "/rest/transactions/upload")
public class TransactionsUploadController {

    final Logger logger = LoggerFactory.getLogger(TransactionsUploadController.class);

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private final BankTransactionsService bankTransactionsService;
    private final TransactionsService transactionsService;

    @Autowired
    public TransactionsUploadController(
            BankTransactionsService bankTransactionsService,
            TransactionsService transactionsService
    ) {
        this.bankTransactionsService = bankTransactionsService;
        this.transactionsService = transactionsService;
    }

    @PostMapping
    public ResponseEntity<String> uploadTransactions(
            @RequestParam("file") MultipartFile file,
            @RequestParam("account") String account
    ) throws IOException, CsvValidationException {

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            if ("Gunnison Checking".equals(account)) {
                TransactionUploadResults results = parseStateBankData(bufferedReader, account);
                return ResponseEntity.ok(results.toString());
            } else if ("Zions All".equals(account)) {
                TransactionUploadResults results = parseZionsData(bufferedReader, parseCsvLine(bufferedReader.readLine()));
                return ResponseEntity.ok(results.toString());
            } else {
                return ResponseEntity.ok("Unknown account type");
            }
        }

    }

    private TransactionUploadResults parseStateBankData(BufferedReader bufferedReader, String account) throws IOException, CsvValidationException {
        int existingTransactions = 0;
        int newTransactions = 0;
        int failedTransactions = 0;

        String[] headers = parseCsvLine(bufferedReader.readLine());

        String line;
        String[] rowValues;
        BankTransaction bankTransaction;
        while ((line = bufferedReader.readLine()) != null) {
            rowValues = parseCsvLine(line);
            if (bankTransactionsService.exists(account, line)) {
                existingTransactions++;
                continue;
            }

            bankTransaction = new BankTransaction();
            bankTransaction.csv = line;
            bankTransaction.account = account;

            try {
                for (int i = 0; i < rowValues.length; i++) {
                    String column = headers[i];

                    if (column != null) {
                        switch (column) {
                            case "Date":
                                bankTransaction.transactionDate = LocalDate.parse(rowValues[i], DATE_FORMATTER);
                                break;
                            case "Description":
                                bankTransaction.description = rowValues[i];
                                break;
                            case "Comments":
                                bankTransaction.comments = rowValues[i];
                                break;
                            case "Check Number":
                                bankTransaction.checkNumber = rowValues[i];
                                break;
                            case "Amount":
                                bankTransaction.amount = parseMoney(rowValues[i]);
                                break;
                        }
                    }
                }

                if (bankTransactionsService.add(bankTransaction)) {
                    if (transactionsService.addFrom(bankTransaction)) {
                        newTransactions++;
                    } else {
                        logger.info("Failed to add to transactions table");
                        failedTransactions++;
                    }
                } else {
                    logger.info("Failed to add to bank_transactions table");
                    failedTransactions++;
                }
            } catch (Throwable throwable) {
                logger.error("Failed", throwable);
            }
        }

        return new TransactionUploadResults(newTransactions, existingTransactions, failedTransactions);
    }

    private TransactionUploadResults parseZionsData(BufferedReader bufferedReader, String[] accountHeaders) throws CsvValidationException, IOException {
        String accountValues = bufferedReader.readLine();

        String[] headers = parseCsvLine(bufferedReader.readLine());

        int existingTransactions = 0;
        int newTransactions = 0;
        int failedTransactions = 0;

        String line;
        String[] rowValues;
        BankTransaction bankTransaction;
        while ((line = bufferedReader.readLine()) != null) {
            rowValues = parseCsvLine(line);
            if ("Account Number".equals(rowValues[0])) {
                TransactionUploadResults transactionUploadResults = parseZionsData(bufferedReader, rowValues);
                return new TransactionUploadResults(
                        newTransactions + transactionUploadResults.newTransactions(),
                        existingTransactions + transactionUploadResults.existingTransactions(),
                        failedTransactions + transactionUploadResults.failedTransactions()
                );
            }

            bankTransaction = new BankTransaction();
            bankTransaction.csv = line;

            try {
                for (int i = 0; i < rowValues.length; i++) {
                    String column = headers[i];

                    if (column != null) {
                        switch (column) {
                            case "Date":
                                bankTransaction.transactionDate = LocalDate.parse(rowValues[i], DATE_FORMATTER);
                                break;
                            case "Account":
                                bankTransaction.account = rowValues[i];
                                break;
                            case "Description":
                                bankTransaction.description = rowValues[i];
                                break;
                            case "Check #":
                                bankTransaction.checkNumber = rowValues[i];
                                break;
                            case "Memo":
                                bankTransaction.comments = rowValues[i];
                                break;
                            case "Credit":
                            case "Debit":
                                String amount = rowValues[i];
                                if (!amount.isEmpty()) {
                                    bankTransaction.amount = parseMoney(amount);
                                }
                                break;
                        }
                    }
                }

                // TODO figure out how to better check for duplicates
                if (bankTransactionsService.exists(bankTransaction.account, bankTransaction.csv)) {
                    existingTransactions++;
                } else {
                    if (bankTransactionsService.add(bankTransaction)) {
                        bankTransaction.description = cleanZionsDescription(bankTransaction.description);
                        if (transactionsService.addFrom(bankTransaction)) {
                            newTransactions++;
                        } else {
                            logger.info("Failed to add to transactions table");
                            failedTransactions++;
                        }
                    } else {
                        logger.info("Failed to add to bank_transactions table");
                        failedTransactions++;
                    }
                }
            } catch (Throwable throwable) {
                logger.error("Failed", throwable);
            }
        }

        return new TransactionUploadResults(
                newTransactions,
                existingTransactions,
                failedTransactions);
    }

    public static String cleanZionsDescription(String description) {
        if (description != null) {
            int openParen = description.indexOf("(");
            if (openParen != -1) {
                int closingParen = description.indexOf(")", openParen);
                if (closingParen != -1) {
                    return description.substring(openParen + 1, closingParen);
                }
            }
        }

        return description;
    }

    private static int parseMoney(String string) {
        if (string == null || string.isEmpty()) {
            return 0;
        }

        String replaced = string.replaceAll("[^0-9.-]", "");
        double number = Double.parseDouble(replaced);
        return (int) (number * 100);
    }

    private static String[] parseCsvLine(String line) throws IOException, CsvValidationException {
        if (line != null && !line.isEmpty()) {
            try (CSVReader strings = new CSVReader(new StringReader(line))) {
                return strings.readNext();
            }
        }

        return null;
    }

}
