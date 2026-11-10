package sigpesq.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = EnvLoader.get("DB_URL");
    private static final String USER = EnvLoader.get("DB_USER");
    private static final String PASS = EnvLoader.get("DB_PASS");

    private DatabaseConnection() {
        // Construtor privado para evitar instanciacao
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}