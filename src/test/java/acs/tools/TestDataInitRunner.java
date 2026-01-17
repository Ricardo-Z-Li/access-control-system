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

public class TestDataInitRunner {

    public static void main(String[] args) throws Exception {
        Path configPath = Paths.get(System.getProperty("user.dir"),
                "src", "test", "resources", "application-test.properties");
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
                    "src", "test", "resources", "db", "init-test-data.sql").toString();

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

        System.out.println("Test data initialization completed.");
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
                || lower.contains("exists")
                || lower.contains("foreign key constraint")
                || lower.contains("cannot add or update a child row")
                || lower.contains("cannot delete or update a parent row")
                || lower.contains("integrity constraint violation");
    }
    
    private static List<String> splitStatements(String sql) throws IOException {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inComment = false;
        
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : 0;
            
            if (!inComment && c == '-' && next == '-') {
                inComment = true;
                i++; // Skip next dash
                continue;
            }
            
            if (inComment && (c == '\n' || c == '\r')) {
                inComment = false;
                if (c == '\r' && next == '\n') {
                    i++; // Skip \n after \r
                }
                continue;
            }
            
            if (inComment) {
                continue;
            }
            
            if (c == ';') {
                String statement = current.toString().trim();
                if (!statement.isEmpty()) {
                    statements.add(statement);
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        
        String lastStatement = current.toString().trim();
        if (!lastStatement.isEmpty()) {
            statements.add(lastStatement);
        }
        
        return statements;
    }
}