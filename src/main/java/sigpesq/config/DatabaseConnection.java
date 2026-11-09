package br.ufes.sigpesq.config; //Checar o caminho do pacote, pois pode variar dependendo da estrutura do projeto

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DatabaseConnection {
    private static final Map<String, String> env = new HashMap<>();

    static {
        try {
            for (String line : Files.readAllLines(Paths.get(".env"))) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#") && line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    env.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler o arquivo .env na raiz do projeto", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            env.get("DB_URL"),
            env.get("DB_USER"),
            env.get("DB_PASS")
        );
    }
}