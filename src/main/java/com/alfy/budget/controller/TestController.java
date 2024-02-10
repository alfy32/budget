package com.alfy.budget.controller;

import com.alfy.budget.csv.StateBankCsvParser;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping(path = "/test")
public class TestController {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public TestController(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @GetMapping(path = "/transactions")
    public List<String> test() {
        return namedParameterJdbcTemplate.query("SELECT * FROM transactions", (ResultSet rs, int rowNum) -> {
            return rs.getString("id") + "|" + rs.getString("transaction_date") + "|" + rs.getString("account") + "|" + rs.getString("description") + "|" + rs.getString("amount");
        });
    }

    @GetMapping(path = "/load")
    public void load() throws IOException, CsvValidationException {
        String home = System.getProperty("user.home");
        Path downloadsFolder = Paths.get(home).resolve("Downloads");

        Optional<Path> mostRecentFile = StateBankCsvParser.getMostRecentFile(downloadsFolder);
        if (mostRecentFile.isPresent()) {
            Path path = mostRecentFile.get();
            System.out.println("MostRecent Download: " + path);

            String sql = "INSERT INTO transactions (account, transaction_date, comments, check_number, amount)" +
                    "VALUES (:account, :date, :comments, :check_number, :amount)";

            StateBankCsvParser parser = new StateBankCsvParser();
            List<Map<String, String>> parse = parser.parse(path);
            for (Map<String, String> stringStringMap : parse) {
//                MapSqlParameterSource params = new MapSqlParameterSource();
//                params.addValue("account", stringStringMap.get("Gunnison Checking"), );
//                params.addValue("date", stringStringMap.get("Date"));
//                params.addValue("comments", stringStringMap.get("Comments"));
//                params.addValue("check_number", stringStringMap.get("Check Number"));
//                params.addValue("amount", stringStringMap.get("Amount"));

                HashMap<String, Object> rowValues = new HashMap<>();
                rowValues.put("account", stringStringMap.get("Gunnison Checking"));
                rowValues.put("date", LocalDate.parse(stringStringMap.get("Date"), DateTimeFormatter.BASIC_ISO_DATE));
                rowValues.put("comments", stringStringMap.get("Comments"));
                rowValues.put("check_number", stringStringMap.get("Check Number"));
                rowValues.put("amount", stringStringMap.get("Amount"));
                namedParameterJdbcTemplate.update(sql, rowValues);
            }
        }
    }

}
