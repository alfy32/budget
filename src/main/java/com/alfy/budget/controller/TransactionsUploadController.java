package com.alfy.budget.controller;

import com.alfy.budget.model.TransactionUploadResults;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

@RestController
@RequestMapping(path = "/rest/transactions/upload")
public class TransactionsUploadController {

    final Logger logger = LoggerFactory.getLogger(TransactionsUploadController.class);
    public static final String SQL_INSERT_INTO_BANK_TRANSACTIONS = "INSERT INTO bank_transactions (" +
            "  account," +
            "  transaction_date," +
            "  description," +
            "  comments," +
            "  check_number," +
            "  amount" +
            ")" +
            "VALUES (" +
            "  :account," +
            "  :date," +
            "  :description," +
            "  :comments," +
            "  :check_number," +
            "  :amount" +
            ")";

    public static final String SQL_INSERT_INTO_TRANSACTIONS = "INSERT INTO transactions (" +
            "  bank_transaction_id," +
            "  account," +
            "  transaction_date," +
            "  description," +
            "  comments," +
            "  check_number," +
            "  amount" +
            ")" +
            "VALUES (" +
            "  :bank_transaction_id," +
            "  :account," +
            "  :date," +
            "  :description," +
            "  :comments," +
            "  :check_number," +
            "  :amount" +
            ")";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public TransactionsUploadController(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Transactional
    private boolean addTransactionToDatabase(HashMap<String, Object> paramMap) {
        // TODO figure out how to avoid duplicates if I upload the same transaction from the same bank.

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int rowsAdded = namedParameterJdbcTemplate.update(SQL_INSERT_INTO_BANK_TRANSACTIONS, new MapSqlParameterSource(paramMap), keyHolder);
        if (rowsAdded != 1) {
            logger.info("Failed to add to bank_transactions table");
            return false;
        }

        int transactionId = (int) keyHolder.getKeys().get("id");
        logger.info("Added new transaction: transactionId=" + transactionId);

        paramMap.put("bank_transaction_id", transactionId);
        rowsAdded = namedParameterJdbcTemplate.update(SQL_INSERT_INTO_TRANSACTIONS, paramMap);
        if (rowsAdded != 1) {
            logger.info("Failed to add to transactions table");
            return false;
        }

        return true;
    }

    @PostMapping()
    public ResponseEntity<String> uploadTransactions(
            @RequestParam("file") MultipartFile file,
            @RequestParam("account") String account
    ) throws IOException, CsvValidationException {

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            if ("Gunnison Checking".equals(account)) {
                TransactionUploadResults results = parseStateBankData(csvReader, account);
                return ResponseEntity.ok(results.toString());
            } else if ("Zions All".equals(account)) {
                TransactionUploadResults results = parseZionsData(csvReader, csvReader.readNext());
                return ResponseEntity.ok(results.toString());
            } else {
                return ResponseEntity.ok("Unknown account type");
            }
        }

    }

    private TransactionUploadResults parseStateBankData(CSVReader csvReader, String account) throws IOException, CsvValidationException {
        int existingTransactions = 0;
        int newTransactions = 0;

        HashMap<String, Object> paramMap = new HashMap<>();

        String[] headers = csvReader.readNext();

        String[] rowValues;
        while ((rowValues = csvReader.readNext()) != null) {
            paramMap.clear();
            paramMap.put("account", account);

            try {
                for (int i = 0; i < rowValues.length; i++) {
                    String column = headers[i];

                    if (column != null) {
                        switch (column) {
                            case "Date":
                                paramMap.put("date", LocalDate.parse(rowValues[i], DATE_FORMATTER));
                                break;
                            case "Description":
                                paramMap.put("description", rowValues[i]);
                                break;
                            case "Comments":
                                paramMap.put("comments", rowValues[i]);
                                break;
                            case "Check Number":
                                paramMap.put("check_number", rowValues[i]);
                                break;
                            case "Amount":
                                paramMap.put("amount", parseMoney(rowValues[i]));
                                break;
                        }
                    }
                }

                paramMap.putIfAbsent("description", null);
                paramMap.putIfAbsent("comments", null);
                paramMap.putIfAbsent("check_number", null);
                if (addTransactionToDatabase(paramMap)) {
                    newTransactions++;
                } else {
                    existingTransactions++;
                }
            } catch (Throwable throwable) {
                logger.error("Failed", throwable);
            }
        }

        return new TransactionUploadResults(newTransactions, existingTransactions);
    }

    private TransactionUploadResults parseZionsData(CSVReader csvReader, String[] accountHeaders) throws CsvValidationException, IOException {
        String[] accountValues = csvReader.readNext();

        String[] headers = csvReader.readNext();

        int existingTransactions = 0;
        int newTransactions = 0;
        HashMap<String, Object> paramMap = new HashMap<>();

        String[] rowValues;
        while ((rowValues = csvReader.readNext()) != null) {
            if ("Account Number".equals(rowValues[0])) {
                TransactionUploadResults transactionUploadResults = parseZionsData(csvReader, rowValues);
                return new TransactionUploadResults(
                        newTransactions + transactionUploadResults.getNewTransactions(),
                        existingTransactions + transactionUploadResults.getExistingTransactions()
                );
            }

            paramMap.clear();

            try {
                for (int i = 0; i < rowValues.length; i++) {
                    String column = headers[i];

                    if (column != null) {
                        switch (column) {
                            case "Date":
                                paramMap.put("date", LocalDate.parse(rowValues[i], DATE_FORMATTER));
                                break;
                            case "Account":
                                paramMap.put("account", rowValues[i]);
                                break;
                            case "Description":
                                paramMap.put("description", rowValues[i]);
                                break;
                            case "Check #":
                                paramMap.put("check_number", rowValues[i]);
                                break;
                            case "Memo":
                                paramMap.put("comments", rowValues[i]);
                                break;
                            case "Credit":
                            case "Debit":
                                String amount = rowValues[i];
                                if (!amount.isEmpty()) {
                                    paramMap.put("amount", parseMoney(amount));
                                }
                                break;
                        }
                    }
                }

                paramMap.putIfAbsent("description", null);
                paramMap.putIfAbsent("comments", null);
                paramMap.putIfAbsent("check_number", null);
                if (addTransactionToDatabase(paramMap)) {
                    newTransactions++;
                } else {
                    existingTransactions++;
                }
            } catch (Throwable throwable) {
                logger.error("Failed", throwable);
            }
        }

        return new TransactionUploadResults(
                newTransactions,
                existingTransactions
        );
    }

    private static int parseMoney(String string) {
        if (string == null || string.isEmpty()) {
            return 0;
        }

        String replaced = string.replaceAll("[^0-9.-]", "");
        double number = Double.parseDouble(replaced);
        return (int) (number * 100);
    }

}
