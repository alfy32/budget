package com.alfy.budget.controller;

import com.alfy.budget.model.BankTransaction;
import com.alfy.budget.model.TransactionUploadResults;
import com.alfy.budget.service.AutoCategorizeService;
import com.alfy.budget.service.BankTransactionsService;
import com.alfy.budget.service.TransactionsService;
import com.alfy.budget.tools.Tools;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping(path = "/rest/transactions/upload")
public class TransactionsUploadController {

    final Logger logger = LoggerFactory.getLogger(TransactionsUploadController.class);

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    public static final DateTimeFormatter DATE_FORMATTER_DFCU = DateTimeFormatter.ofPattern("M/d/yyyy");

    private final BankTransactionsService bankTransactionsService;
    private final TransactionsService transactionsService;
    private final AutoCategorizeService autoCategorizeService;

    @Autowired
    public TransactionsUploadController(
            BankTransactionsService bankTransactionsService,
            TransactionsService transactionsService,
            AutoCategorizeService autoCategorizeService
    ) {
        this.bankTransactionsService = bankTransactionsService;
        this.transactionsService = transactionsService;
        this.autoCategorizeService = autoCategorizeService;
    }

    @PostMapping(
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Object> uploadTransactions(
            @RequestParam("file") MultipartFile file,
            @RequestParam("account") String account
    ) throws IOException, CsvValidationException {

        autoCategorizeService.updateCachedData();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            switch (account) {
                case "Gunnison Checking" -> {
                    TransactionUploadResults results = parseStateBankData(bufferedReader, account);
                    return ResponseEntity.ok(results);
                }
                case "Zions Cash Back Visa" -> {
                    TransactionUploadResults results = parseZionsCreditCard(bufferedReader, "Cash Back Visa");
                    return ResponseEntity.ok(results);
                }
                case "First Choice Platinum" -> {
                    TransactionUploadResults results = parseDFCU(bufferedReader, account);
                    return ResponseEntity.ok(results);
                }
                case "Zions All" -> {
                    TransactionUploadResults results = parseZionsData(bufferedReader, parseCsvLine(bufferedReader.readLine()));
                    return ResponseEntity.ok(results);
                }
                case null, default -> {
                    return ResponseEntity.ok("Unknown account type");
                }
            }
        }

    }

    private TransactionUploadResults parseStateBankData(
            BufferedReader bufferedReader,
            String account
    ) throws IOException, CsvValidationException {
        int existingTransactions = 0;
        int newTransactions = 0;
        int failedTransactions = 0;
        int autoCategorizedTransactions = 0;

        String[] headers = parseCsvLine(bufferedReader.readLine());

        String line;
        String[] rowValues;
        BankTransaction bankTransaction;
        while ((line = bufferedReader.readLine()) != null) {
            rowValues = parseCsvLine(line);

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
                                BigDecimal parsedAmount = parseMoney(rowValues[i]);
                                bankTransaction.amount = parsedAmount.abs();
                                bankTransaction.transactionType = Tools.isLessThanZero(parsedAmount) ? "debit" : "credit";
                                break;
                            case "Balance":
                                bankTransaction.balance = parseMoney(rowValues[i]);
                                break;
                        }
                    }
                }

                if (bankTransactionsService.exists(account, bankTransaction)) {
                    existingTransactions++;
                    continue;
                }

                if (bankTransactionsService.add(bankTransaction)) {
                    UUID transactionId = transactionsService.addFrom(bankTransaction);
                    if (transactionId != null) {
                        newTransactions++;
                        boolean autoCategorized = autoCategorizeService.autoCategorize(bankTransaction, transactionId);
                        if (autoCategorized) {
                            autoCategorizedTransactions++;
                        }
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

        return new TransactionUploadResults(newTransactions, existingTransactions, failedTransactions, autoCategorizedTransactions);
    }

    private TransactionUploadResults parseZionsCreditCard(
            BufferedReader bufferedReader,
            String account
    ) throws IOException, CsvValidationException {
        int existingTransactions = 0;
        int newTransactions = 0;
        int failedTransactions = 0;
        int autoCategorizedTransactions = 0;

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
                            case "Transaction Date":
                                bankTransaction.transactionDate = LocalDate.parse(rowValues[i]);
                                break;
                            case "Post Date":
                                bankTransaction.postDate = LocalDate.parse(rowValues[i]);
                                break;
                            case "Transaction Detail":
                                bankTransaction.description = rowValues[i];
                                break;
                            case "Amount":
                                BigDecimal parsedAmount = parseMoney(rowValues[i]);
                                bankTransaction.amount = parsedAmount.abs();
                                bankTransaction.transactionType = Tools.isLessThanZero(parsedAmount) ? "credit" : "debit";
                                break;
                        }
                    }
                }

                if (bankTransactionsService.add(bankTransaction)) {
                    UUID transactionId = transactionsService.addFrom(bankTransaction);
                    if (transactionId != null) {
                        newTransactions++;
                        boolean autoCategorized = autoCategorizeService.autoCategorize(bankTransaction, transactionId);
                        if (autoCategorized) {
                            autoCategorizedTransactions++;
                        }
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

        return new TransactionUploadResults(newTransactions, existingTransactions, failedTransactions, autoCategorizedTransactions);
    }

    private TransactionUploadResults parseDFCU(
            BufferedReader bufferedReader,
            String account
    ) throws IOException, CsvValidationException {
        int existingTransactions = 0;
        int newTransactions = 0;
        int failedTransactions = 0;
        int autoCategorizedTransactions = 0;

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
                            case "Posting Date" -> bankTransaction.postDate = LocalDate.parse(rowValues[i], DATE_FORMATTER_DFCU);
                            case "Effective Date" -> bankTransaction.transactionDate = LocalDate.parse(rowValues[i], DATE_FORMATTER_DFCU);
                            case "Transaction Type" -> {
                                if (rowValues[i] == null) {
                                    bankTransaction.transactionType = "credit";
                                } else {
                                    bankTransaction.transactionType = rowValues[i].toLowerCase();
                                }
                            }
                            case "Amount" -> bankTransaction.amount = parseMoney(rowValues[i]).abs();
                            case "Check Number" -> bankTransaction.checkNumber = rowValues[i];
                            case "Description" -> bankTransaction.description = rowValues[i];
                        }
                    }
                }

                if (bankTransactionsService.add(bankTransaction)) {
                    UUID transactionId = transactionsService.addFrom(bankTransaction);
                    if (transactionId != null) {
                        newTransactions++;
                        boolean autoCategorized = autoCategorizeService.autoCategorize(bankTransaction, transactionId);
                        if (autoCategorized) {
                            autoCategorizedTransactions++;
                        }
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

        return new TransactionUploadResults(newTransactions, existingTransactions, failedTransactions, autoCategorizedTransactions);
    }

    private TransactionUploadResults parseZionsData(
            BufferedReader bufferedReader,
            String[] accountHeaders
    ) throws CsvValidationException, IOException {
        String accountValues = bufferedReader.readLine();

        String[] headers = parseCsvLine(bufferedReader.readLine());

        int existingTransactions = 0;
        int newTransactions = 0;
        int failedTransactions = 0;
        int autoCategorizedTransactions = 0;

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
                        failedTransactions + transactionUploadResults.failedTransactions(),
                        autoCategorizedTransactions + transactionUploadResults.autoCategorizedTransactions()
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
                            case "Credit": {
                                String value = rowValues[i];
                                if (!value.isEmpty()) {
                                    bankTransaction.amount = parseMoney(value).abs();
                                    bankTransaction.transactionType = "credit";
                                }
                                break;
                            }
                            case "Debit": {
                                String value = rowValues[i];
                                if (!value.isEmpty()) {
                                    bankTransaction.amount = parseMoney(value).abs();
                                    bankTransaction.transactionType = "debit";
                                }
                                break;
                            }
                        }
                    }
                }

                // TODO figure out how to better check for duplicates
                if (bankTransactionsService.exists(bankTransaction.account, bankTransaction.csv)) {
                    existingTransactions++;
                } else {
                    if (bankTransactionsService.add(bankTransaction)) {
                        bankTransaction.description = cleanZionsDescription(bankTransaction.description);
                        UUID transactionId = transactionsService.addFrom(bankTransaction);
                        if (transactionId != null) {
                            newTransactions++;
                            boolean autoCategorized = autoCategorizeService.autoCategorize(bankTransaction, transactionId);
                            if (autoCategorized) {
                                autoCategorizedTransactions++;
                            }
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
                failedTransactions,
                autoCategorizedTransactions
        );
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

    public static BigDecimal parseMoney(String string) {
        if (string == null || string.isEmpty()) {
            return BigDecimal.ZERO;
        }

        String replaced = string.replaceAll("[^0-9.-]", "");
        return new BigDecimal(replaced);
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
