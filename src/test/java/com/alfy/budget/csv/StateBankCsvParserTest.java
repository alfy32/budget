package com.alfy.budget.csv;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

class StateBankCsvParserTest {

    @Test
    public void test() throws Exception {
        String home = System.getProperty("user.home");
        Path downloadsFolder = Paths.get(home).resolve("Downloads");

        Optional<Path> mostRecentFile = StateBankCsvParser.getMostRecentFile(downloadsFolder);
        if (mostRecentFile.isPresent()) {
            Path path = mostRecentFile.get();
            System.out.println("MostRecent Download: " + path);

            StateBankCsvParser parser = new StateBankCsvParser();
            List<Map<String, String>> parse = parser.parse(path);
            for (Map<String, String> stringStringMap : parse) {
                System.out.println(stringStringMap);
            }
        }
    }





}