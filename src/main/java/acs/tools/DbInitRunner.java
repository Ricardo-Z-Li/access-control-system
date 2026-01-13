package acs.tools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DbInitRunner {

    public static void main(String[] args) throws Exception {
        Path configPath = Paths.get(System.getProperty("user.dir"),
                "src", "main", "resources", "application.properties");
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            props.load(in);
        }

        String url = require(props, "spring.datasource.url");
        String user = require(props, "spring.datasource.username");
        String pass = props.getProperty("spring.datasource.password", "");
        String dbName = extractDbName(url);
        String serverUrl = buildServerUrl(url);

        String sqlPath = args.length > 0
                ? args[0]
                : Paths.get(System.getProperty("user.dir"),
                    "src", "main", "resources", "db", "access_control_db.sql").toString();

        String sql = Files.readString(Paths.get(sqlPath), StandardCharsets.UTF_8);
        sql = stripBom(sql);
        List<String> statements = splitStatements(sql);

        try (Connection conn = DriverManager.getConnection(serverUrl, user, pass);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);
        }

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.toUpperCase().startsWith("USE ")) {
                    continue;
                }
                try {
                    stmt.execute(statement);
                } catch (SQLException e) {
                    if (!isIgnorable(e)) {
                        throw e;
                    }
                }
            }
        }

        System.out.println("Database initialization completed.");
    }

    private static String require(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing property: " + key);
        }
        return value.trim();
    }

    private static String extractDbName(String url) {
        int slash = url.lastIndexOf('/');
        if (slash < 0 || slash == url.length() - 1) {
            throw new IllegalArgumentException("Invalid JDBC URL: " + url);
        }
        int q = url.indexOf('?', slash);
        return q >= 0 ? url.substring(slash + 1, q) : url.substring(slash + 1);
    }

    private static String buildServerUrl(String url) {
        int slash = url.lastIndexOf('/');
        if (slash < 0) {
            throw new IllegalArgumentException("Invalid JDBC URL: " + url);
        }
        String prefix = url.substring(0, slash + 1);
        int q = url.indexOf('?', slash);
        if (q >= 0) {
            String params = url.substring(q);
            return prefix + params;
        }
        return prefix;
    }

    private static String stripBom(String text) {
        if (text != null && !text.isEmpty() && text.charAt(0) == '﻿') {
            return text.substring(1);
        }
        return text;
    }


    private static boolean isIgnorable(SQLException e) {
        String msg = e.getMessage();
        if (msg == null) {
            return false;
        }
        String lower = msg.toLowerCase();
        return lower.contains("already exists")
                || lower.contains("duplicate")
                || lower.contains("duplicate column")
                || lower.contains("duplicate foreign key")
                || lower.contains("duplicate key")
                || lower.contains("exists");
    }
    private static List<String> splitStatements(String sql) throws IOException {
        String[] lines = sql.split("\r?\n");
        StringBuilder buffer = new StringBuilder();
        List<String> statements = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith("--") || trimmed.startsWith("#")) {
                continue;
            }
            buffer.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                String statement = buffer.toString().trim();
                if (statement.endsWith(";")) {
                    statement = statement.substring(0, statement.length() - 1);
                }
                statements.add(statement);
                buffer.setLength(0);
            }
        }
        if (buffer.length() > 0) {
            statements.add(buffer.toString().trim());
        }
        return statements;
    }
}
