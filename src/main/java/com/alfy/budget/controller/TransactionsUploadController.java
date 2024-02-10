package com.alfy.budget.controller;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

@RestController
@RequestMapping(path = "/transactions/upload")
public class TransactionsUploadController {

    final Logger logger = LoggerFactory.getLogger(TransactionsUploadController.class);
    public static final String SQL = "INSERT INTO transactions (account, transaction_date, description, comments, check_number, amount)" +
            "VALUES (:account, :date, :description, :comments, :check_number, :amount)";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public TransactionsUploadController(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @PostMapping()
    public ResponseEntity<String> uploadTransactions(
            @RequestParam("file") MultipartFile file,
            @RequestParam("account") String account
    ) throws IOException, CsvValidationException {
        int rowsAdded = 0;

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            if ("Gunnison Checking".equals(account)) {
                rowsAdded = parseStateBankData(csvReader, account);
            } else if ("Zions All".equals(account)) {
               rowsAdded = parseZionsData(csvReader, csvReader.readNext());
            } else {

            }
        }

        return ResponseEntity.ok("Got it");
    }

    private int parseStateBankData(CSVReader csvReader, String account) throws IOException, CsvValidationException {
        int rowsAdded = 0;
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
                namedParameterJdbcTemplate.update(SQL, paramMap);
                rowsAdded++;
            } catch (Throwable throwable) {
                logger.error("Failed", throwable);
            }
        }

        return rowsAdded;
    }

    private int parseZionsData(CSVReader csvReader, String[] accountHeaders) throws CsvValidationException, IOException {
        String[] accountValues = csvReader.readNext();

        String[] headers = csvReader.readNext();

        int rowsAdded = 0;
        HashMap<String, Object> paramMap = new HashMap<>();

        String[] rowValues;
        while ((rowValues = csvReader.readNext()) != null) {
            if ("Account Number".equals(rowValues[0])) {
                return rowsAdded + parseZionsData(csvReader, rowValues);
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
                namedParameterJdbcTemplate.update(SQL, paramMap);
                rowsAdded++;
            } catch (Throwable throwable) {
                logger.error("Failed", throwable);
            }
        }

        return rowsAdded;
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
