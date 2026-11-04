package com.alfy.budget.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SQLite stores ids and dates as plain text. These helpers convert in both directions.
 */
final class Db {

    private Db() {
    }

    static String id(UUID id) {
        return id == null ? null : id.toString();
    }

    static String date(LocalDate date) {
        return date == null ? null : date.toString();
    }

    static UUID getId(ResultSet resultSet, String column) throws SQLException {
        String value = resultSet.getString(column);
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    static LocalDate getDate(ResultSet resultSet, String column) throws SQLException {
        String value = resultSet.getString(column);
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

}
