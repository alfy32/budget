package com.alfy.budget.config;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SQLite creates the database file on first use, but not the folder it lives in.
 * This makes the folder right before the database is first opened.
 */
@Configuration
public class DatabaseFolder {

    @Bean
    static BeanPostProcessor createDatabaseFolder(Environment environment) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource) {
                    Path folder = Path.of(environment.getRequiredProperty("budget.db-path")).toAbsolutePath().getParent();
                    try {
                        Files.createDirectories(folder);
                    } catch (IOException e) {
                        throw new UncheckedIOException("Could not create database folder " + folder, e);
                    }
                }
                return bean;
            }
        };
    }

}
