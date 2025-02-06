package com.futplay;

import static spark.Spark.*;
import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // Inicializar la base de datos
        DatabaseService.initializeDatabase();

        // Configurar el puerto del servidor
        port(4567);

        // Configuración estática (para recursos como JS, CSS, imágenes)
        staticFiles.location("/public");
        
        // Configurar sesiones
        before((req, res) -> {
            if (req.pathInfo().startsWith("/privado")) {
                String usuario = req.session().attribute("usuario");
                if (usuario == null) {
                    res.redirect("/login");
                    halt();
                }
            }
        });

        // Manejar login (POST)
        post("/login", (req, res) -> {
            String email = req.queryParams("email");
            String password = req.queryParams("password");
            
            try (Connection conn = DatabaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM usuarios WHERE email = ? AND password = ?")) {
                stmt.setString(1, email);
                stmt.setString(2, password); // Deberías usar hash en la práctica real
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    req.session().attribute("usuario", email);
                    res.redirect("/");
                } else {
                    res.redirect("/login?error=1");
                }
            }
            return null; // Necesario para devolver algo
        });

        // Crear nuevo partido (POST)
        post("/partidos/nuevo", (req, res) -> {
            String creador = req.session().attribute("usuario");
            if (creador == null) return "Acceso no autorizado";
            
            try (Connection conn = DatabaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO partidos (creador_email, fecha, hora, ubicacion, tipoCampo, maxJugadores) VALUES (?,?,?,?,?,?)")) {
                 
                stmt.setString(1, creador);
                stmt.setDate(2, Date.valueOf(req.queryParams("fecha")));
                stmt.setTime(3, Time.valueOf(req.queryParams("hora") + ":00"));
                stmt.setString(4, req.queryParams("ubicacion"));
                stmt.setString(5, "11vs11"); // Modificar según formulario
                stmt.setInt(6, Integer.parseInt(req.queryParams("maxJugadores")));
                
                stmt.executeUpdate();
                res.redirect("/partidos");
            }
            return null; // Necesario para devolver algo
        });

        // Página de partidos (GET)
        get("/partidos", (req, res) -> {
            // Listar partidos desde la base de datos
            StringBuilder partidosHtml = new StringBuilder();
            try (Connection conn = DatabaseService.getConnection();
                 Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT * FROM partidos WHERE fecha >= CURDATE() ORDER BY fecha");
                while (rs.next()) {
                    partidosHtml.append("<div>")
                            .append("<h3>").append(rs.getString("ubicacion")).append("</h3>")
                            .append("<p>").append(rs.getDate("fecha")).append(" a las ").append(rs.getTime("hora")).append("</p>")
                            .append("</div>");
                }
            }

            Map<String, Object> model = new HashMap<>();
            model.put("partidos", partidosHtml.toString());
            return new ModelAndView(model, "templates/partidos.vtl");
        }, new VelocityTemplateEngine());

        // Página de inicio (GET)
        get("/", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("titulo", "Bienvenido a FutPlay");
            model.put("mensaje", "Organiza partidos y torneos en tu zona");
            return new ModelAndView(model, "templates/index.vtl");
        }, new VelocityTemplateEngine());

        // Página de registro (GET)
        get("/registro", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("titulo", "Registro de usuario");
            return new ModelAndView(model, "templates/registro.vtl");
        }, new VelocityTemplateEngine());

        // Página de login (GET)
        get("/login", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("titulo", "Iniciar sesión");
            return new ModelAndView(model, "templates/login.vtl");
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
                stmt.setString(3, password); // Asegúrate de usar hash en la práctica real
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                return "Error al registrar usuario";
            }

            res.redirect("/");
            return null; // Necesario para devolver algo
        });
    }
}
