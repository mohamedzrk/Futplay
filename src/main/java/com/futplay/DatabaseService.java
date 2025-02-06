package com.futplay;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseService {

    private static HikariDataSource dataSource;

    // Inicializar el DataSource con HikariCP
    public static void initDataSource() {
        // Puedes externalizar estos parámetros con variables de entorno si lo deseas
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:h2:./futplaydb;AUTO_SERVER=TRUE");
        String dbUser = System.getenv().getOrDefault("DB_USER", "sa");
        String dbPass = System.getenv().getOrDefault("DB_PASS", "");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPass);
        // Parámetros opcionales: config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            initDataSource();
        }
        return dataSource.getConnection();
    }

    // Método para inicializar la base de datos (creación de tablas)
    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Tabla de usuarios
            String sqlUsuarios = "CREATE TABLE IF NOT EXISTS usuarios ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "nombre VARCHAR(50) NOT NULL, "
                    + "email VARCHAR(100) NOT NULL UNIQUE, "
                    + "password VARCHAR(100) NOT NULL"
                    + ");";
            stmt.execute(sqlUsuarios);

            // Tabla de partidos (por simplicidad, sin modificaciones)
            String sqlPartidos = "CREATE TABLE IF NOT EXISTS partidos ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "creador_email VARCHAR(100) NOT NULL, "
                    + "fecha DATE NOT NULL, "
                    + "hora TIME NOT NULL, "
                    + "ubicacion VARCHAR(100) NOT NULL, "
                    + "tipoCampo VARCHAR(50) NOT NULL, "
                    + "maxJugadores INT DEFAULT 10, "
                    + "descripcion TEXT"
                    + ");";
            stmt.execute(sqlPartidos);

            // Tabla de torneos (por simplicidad, sin modificaciones)
            String sqlTorneos = "CREATE TABLE IF NOT EXISTS torneos ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "creador_email VARCHAR(100) NOT NULL, "
                    + "nombre VARCHAR(100) NOT NULL, "
                    + "fechaInicio DATE NOT NULL, "
                    + "fechaFin DATE NOT NULL, "
                    + "descripcion TEXT"
                    + ");";
            stmt.execute(sqlTorneos);

            System.out.println("Base de datos inicializada correctamente.");
        } catch (SQLException e) {
            System.err.println("Error inicializando la base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
