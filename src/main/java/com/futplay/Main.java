package com.futplay;

import static spark.Spark.*;
import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // Inicializar la base de datos
        DatabaseService.initializeDatabase();

        // Configurar el puerto del servidor
        port(4567);

        // Página de inicio
        get("/", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("titulo", "Bienvenido a FutPlay");
            model.put("mensaje", "Organiza partidos y torneos en tu zona");
            return new ModelAndView(model, "templates/index.vtl");
        }, new VelocityTemplateEngine());

        // Página de registro
        get("/registro", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("titulo", "Registro de usuario");
            return new ModelAndView(model, "templates/registro.vtl");
        }, new VelocityTemplateEngine());

        // Página de login
        get("/login", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("titulo", "Iniciar sesión");
            return new ModelAndView(model, "templates/login.vtl");
        }, new VelocityTemplateEngine());

        // Página de partidos
        get("/partidos", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("titulo", "Partidos disponibles");
            return new ModelAndView(model, "templates/partidos.vtl");
        }, new VelocityTemplateEngine());

        // Página de torneos
        get("/torneos", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("titulo", "Torneos disponibles");
            return new ModelAndView(model, "templates/torneos.vtl");
        }, new VelocityTemplateEngine());

        // Registro de usuario (POST)
        post("/registro", (req, res) -> {
            String nombre = req.queryParams("nombre");
            String email = req.queryParams("email");
            String password = req.queryParams("password");

            try (Connection conn = DatabaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO usuarios (nombre, email, password) VALUES (?, ?, ?)")) {
                stmt.setString(1, nombre);
                stmt.setString(2, email);
                stmt.setString(3, password);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                return "Error al registrar usuario";
            }

            res.redirect("/");
            return null;
        });
    }
}
