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

class ZionsCsvParserTest {

    @Test
    public void test() throws Exception {
        String home = System.getProperty("user.home");
        Path downloadsFolder = Paths.get(home).resolve("Downloads");

        Optional<Path> mostRecentFile = getMostRecentFile(downloadsFolder);
        if (mostRecentFile.isPresent()) {
            Path path = mostRecentFile.get();
            System.out.println("MostRecent Download: " + path);

            ZionsCsvParser parser = new ZionsCsvParser();
            List<Map<String, String>> parse = parser.parse(path);
            for (Map<String, String> stringStringMap : parse) {
                System.out.println(stringStringMap);
            }
        }
    }

    private static Optional<Path> getMostRecentFile(Path downloadsFolder) throws IOException {
        try (Stream<Path> list = Files.list(downloadsFolder)) {
            return list
                    .filter(ZionsCsvParserTest::isCsv)
                    .filter(ZionsCsvParserTest::isZionsFile)
                    .max(Comparator.comparing(ZionsCsvParserTest::extracted));
        }
    }

    private static boolean isCsv(Path path) {
        return path.getFileName().toString().endsWith(".csv");
    }

    private static boolean isZionsFile(Path path) {
        return path.getFileName().toString().startsWith("Transactions-");
    }

    private static FileTime extracted(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException ignore) {
            return FileTime.fromMillis(0);
        }
    }

}