package com.futplay;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseService {

    private static Connection connection;

    // URL para la base de datos H2 en modo embebido
    private static final String DB_URL = "jdbc:h2:./futplaydb;AUTO_SERVER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    // Método para obtener la conexión (se crea si no existe)
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        }
        return connection;
    }

    // Método para inicializar la base de datos y crear las tablas si no existen
    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Crear tabla de usuarios
            String sqlUsuarios = "CREATE TABLE IF NOT EXISTS usuarios ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "nombre VARCHAR(50) NOT NULL, "
                    + "email VARCHAR(100) NOT NULL UNIQUE, "
                    + "password VARCHAR(100) NOT NULL"
                    + ");";
            stmt.execute(sqlUsuarios);

            // Crear tabla de partidos
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

            // Crear tabla de torneos
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

