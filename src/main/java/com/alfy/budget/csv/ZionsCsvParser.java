package com.alfy.budget.csv;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZionsCsvParser {

    public List<Map<String, String>> parse(Path path) throws IOException, CsvValidationException {
        return parse(Files.newInputStream(path));
    }

    public List<Map<String, String>> parse(InputStream inputStream) throws IOException, CsvValidationException {
        List<Map<String, String>> data = new ArrayList<>();

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream))) {
            readAccountData(data, csvReader, csvReader.readNext());
        }

        return data;
    }

    private void readAccountData(List<Map<String, String>> data, CSVReader csvReader, String[] accountHeaders) throws CsvValidationException, IOException {
        String[] accountValues = csvReader.readNext();

        String[] headers = csvReader.readNext();

        String[] lineInArray;
        while ((lineInArray = csvReader.readNext()) != null) {
            if ("Account Number".equals(lineInArray[0])) {
                readAccountData(data, csvReader, lineInArray);
                return;
            }

            Map<String, String> transactionData = new HashMap<>();
            for (int i = 0; i < lineInArray.length; i++) {
                transactionData.put(headers[i], lineInArray[i]);
            }
            data.add(transactionData);
        }
    }
}
