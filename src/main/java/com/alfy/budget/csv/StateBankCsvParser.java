package com.alfy.budget.csv;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

public class StateBankCsvParser {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public List<Map<String, String>> parse(Path path) throws IOException, CsvValidationException {
        return parse(Files.newInputStream(path));
    }

    public List<Map<String, String>> parse(InputStream inputStream) throws IOException, CsvValidationException {
        List<Map<String, String>> data = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            String[] headers = reader.readNext();

            String[] lineInArray;
            while ((lineInArray = reader.readNext()) != null) {
                Map<String, String> lineData = new HashMap<>();
                for (int i = 0; i < lineInArray.length; i++) {
                    lineData.put(headers[i], lineInArray[i]);
                }
                data.add(lineData);
            }
        }

        return data;
    }

    public static Optional<Path> getMostRecentFile(Path downloadsFolder) throws IOException {
        try (Stream<Path> list = Files.list(downloadsFolder)) {
            return list
                    .filter(StateBankCsvParser::isCsv)
                    .filter(StateBankCsvParser::isStateBankFile)
                    .max(Comparator.comparing(StateBankCsvParser::getFileTime));
        }
    }

    private static boolean isCsv(Path path) {
        return path.getFileName().toString().endsWith(".csv");
    }

    private static boolean isStateBankFile(Path path) {
        return path.getFileName().toString().startsWith("export_");
    }

    private static FileTime getFileTime(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException ignore) {
            return FileTime.fromMillis(0);
        }
    }
}
