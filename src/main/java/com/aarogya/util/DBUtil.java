package com.aarogya.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBUtil {
    private static final String H2_SCHEMA = "/database/h2-schema.sql";
    private static final String H2_SEED = "/database/seed.sql";

    public static Connection getConnection() throws SQLException {
        String configuredUrl = System.getProperty("db.url");
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            String defaultUser = configuredUrl.startsWith("jdbc:mysql:") ? "root" : "sa";
            Connection connection = DriverManager.getConnection(
                    configuredUrl,
                    System.getProperty("db.user", defaultUser),
                    System.getProperty("db.password", ""));
            if (configuredUrl.startsWith("jdbc:h2:")) initialize(connection);
            return connection;
        }

        try {
            Class.forName("org.h2.Driver");
            Path databaseDirectory = Path.of(System.getProperty("user.home"), ".aarogya-care");
            Files.createDirectories(databaseDirectory);
            String databaseFile = databaseDirectory.resolve("aarogya").toAbsolutePath().toString();
            Connection connection = DriverManager.getConnection(
                    "jdbc:h2:file:" + databaseFile + ";MODE=MySQL", "sa", "");
            initialize(connection);
            return connection;
        } catch (ClassNotFoundException | IOException e) {
            throw new SQLException("Could not initialize the local Aarogya Clinic database.", e);
        }
    }

    private static void initialize(Connection connection) throws SQLException {
        try {
            ensureInitialized(connection);
        } catch (SQLException failure) {
            try {
                connection.close();
            } catch (SQLException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
            throw failure;
        }
    }

    private static synchronized void ensureInitialized(Connection connection) throws SQLException {
        for (String statement : readStatements(H2_SCHEMA)) {
            if (!statement.isBlank()) {
                try (Statement schema = connection.createStatement()) {
                    schema.execute(statement);
                }
            }
        }

        try (Statement statement = connection.createStatement();
             ResultSet users = statement.executeQuery("SELECT COUNT(*) FROM users")) {
            users.next();
            if (users.getInt(1) == 0) {
                for (String seedStatement : readStatements(H2_SEED)) {
                    if (!seedStatement.isBlank()) {
                        try (Statement seed = connection.createStatement()) {
                            seed.execute(seedStatement);
                        }
                    }
                }
            }
        }
    }

    private static String[] readStatements(String resource) throws SQLException {
        InputStream input = DBUtil.class.getResourceAsStream(resource);
        if (input == null) {
            throw new SQLException("Required database resource is missing: " + resource);
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .reduce("", (left, right) -> left + right + "\n")
                    .split(";");
        } catch (IOException e) {
            throw new SQLException("Could not read database resource: " + resource, e);
        }
    }
}
