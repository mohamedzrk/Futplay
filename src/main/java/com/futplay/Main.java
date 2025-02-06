package com.futplay;

import static spark.Spark.*;

import at.favre.lib.crypto.bcrypt.BCrypt;
import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // Inicializar HikariCP y la base de datos
        DatabaseService.initDataSource();
        DatabaseService.initializeDatabase();

        // Configurar puerto y archivos estáticos
        port(4567);
        staticFiles.location("/public");

        // Filtro para proteger rutas que requieran autenticación (ej. "/privado")
        before((req, res) -> {
            if (req.pathInfo().startsWith("/privado") && req.session().attribute("usuario") == null) {
                res.redirect("/login");
                halt();
            }
        });

        // Página principal
        get("/", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("titulo", "Bienvenido a FutPlay");
            model.put("mensaje", "Organiza partidos y torneos en tu zona");
            return new ModelAndView(model, "templates/index.vtl");
        }, new VelocityTemplateEngine());

        // =====================================================
        // Registro
        // =====================================================
        // Mostrar formulario de registro
        get("/registro", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("titulo", "Registro de usuario");
            // Puedes leer un parámetro de error: req.queryParams("error")
            return new ModelAndView(model, "templates/registro.vtl");
        }, new VelocityTemplateEngine());

        // Procesar el registro (usa BCrypt para hashear la contraseña)
        post("/registro", (req, res) -> {
            String nombre = req.queryParams("nombre");
            String email = req.queryParams("email");
            String password = req.queryParams("password");

            // Hashear la contraseña con BCrypt
            String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());

            try (Connection conn = DatabaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO usuarios (nombre, email, password) VALUES (?, ?, ?)")) {
                stmt.setString(1, nombre);
                stmt.setString(2, email);
                stmt.setString(3, hashedPassword);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                // En caso de error, redirige mostrando un parámetro para indicar el error
                res.redirect("/registro?error=1");
                return null;
            }
            res.redirect("/login");
            return null;
        });

        // =====================================================
        // Login y Logout
        // =====================================================
        // Mostrar formulario de login
        get("/login", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("titulo", "Iniciar sesión");
            return new ModelAndView(model, "templates/login.vtl");
        }, new VelocityTemplateEngine());

        // Procesar el login (usando BCrypt para verificar la contraseña)
        post("/login", (req, res) -> {
            String email = req.queryParams("email");
            String password = req.queryParams("password");

            try (Connection conn = DatabaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT password FROM usuarios WHERE email = ?")) {
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String hashedPassword = rs.getString("password");
                    BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hashedPassword);
                    if (result.verified) {
                        req.session().attribute("usuario", email);
                        res.redirect("/");
                        return null;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            res.redirect("/login?error=1");
            return null;
        });

        // Logout (cerrar sesión)
        get("/logout", (req, res) -> {
            req.session().removeAttribute("usuario");
            res.redirect("/");
            return null;
        });

        // =====================================================
        // Partidos (Ejemplo básico)
        // =====================================================
        // Crear un partido (POST)
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
                stmt.setString(5, "11vs11"); // Ejemplo fijo, se puede modificar
                stmt.setInt(6, Integer.parseInt(req.queryParams("maxJugadores")));
                stmt.executeUpdate();
                res.redirect("/partidos");
            }
            return null;
        });

        // Mostrar partidos (GET)
        get("/partidos", (req, res) -> {
            StringBuilder partidosHtml = new StringBuilder();
            try (Connection conn = DatabaseService.getConnection();
                 Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT * FROM partidos WHERE fecha >= CURDATE() ORDER BY fecha");
                while (rs.next()) {
                    partidosHtml.append("<div class='partido-card'>")
                            .append("<h3>").append(rs.getString("ubicacion")).append("</h3>")
                            .append("<p>").append(rs.getDate("fecha"))
                            .append(" a las ").append(rs.getTime("hora")).append("</p>")
                            .append("</div>");
                }
            }
            Map<String, Object> model = new HashMap<>();
            model.put("partidos", partidosHtml.toString());
            return new ModelAndView(model, "templates/partidos.vtl");
        }, new VelocityTemplateEngine());

        // =====================================================
        // Torneos - CRUD con validación de fechas
        // =====================================================
        // 1. Mostrar formulario para crear nuevo torneo
        get("/torneos/nuevo", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("titulo", "Crear Nuevo Torneo");
            return new ModelAndView(model, "templates/nuevoTorneo.vtl");
        }, new VelocityTemplateEngine());

        // 2. Procesar creación de torneo con validación de fechas
        post("/torneos/nuevo", (req, res) -> {
            String creador = req.session().attribute("usuario");
            if (creador == null) return "Acceso no autorizado";

            String nombre = req.queryParams("nombre");
            String fechaInicioStr = req.queryParams("fechaInicio");
            String fechaFinStr = req.queryParams("fechaFin");
            String descripcion = req.queryParams("descripcion");

            // Convertir a java.sql.Date
            java.sql.Date fechaInicio = Date.valueOf(fechaInicioStr);
            java.sql.Date fechaFin = Date.valueOf(fechaFinStr);

            // Validar que la fecha de fin no sea anterior a la fecha de inicio
            if (fechaFin.before(fechaInicio)) {
                res.redirect("/torneos/nuevo?error=fechasInvalidas");
                return null;
            }

            try (Connection conn = DatabaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO torneos (creador_email, nombre, fechaInicio, fechaFin, descripcion) VALUES (?,?,?,?,?)")) {
                stmt.setString(1, creador);
                stmt.setString(2, nombre);
                stmt.setDate(3, fechaInicio);
                stmt.setDate(4, fechaFin);
                stmt.setString(5, descripcion);
                stmt.executeUpdate();
                res.redirect("/torneos");
            } catch (SQLException e) {
                e.printStackTrace();
                res.redirect("/torneos?nuevo_error=1");
            }
            return null;
        });

        // 3. Mostrar lista de torneos con enlaces para editar y eliminar
        get("/torneos", (req, res) -> {
            StringBuilder torneosHtml = new StringBuilder();
            try (Connection conn = DatabaseService.getConnection();
                 Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT * FROM torneos ORDER BY fechaInicio");
                while (rs.next()) {
                    int id = rs.getInt("id");
                    torneosHtml.append("<div class='col-md-4 mb-4'>")
                            .append("<div class='card h-100'>")
                            .append("<div class='card-body'>")
                            .append("<h5 class='card-title'>").append(rs.getString("nombre")).append("</h5>")
                            .append("<p class='card-text'>Del: ").append(rs.getDate("fechaInicio"))
                            .append(" a ").append(rs.getDate("fechaFin")).append("</p>")
                            .append("<p class='card-text'>").append(rs.getString("descripcion")).append("</p>")
                            .append("</div>")
                            .append("<div class='card-footer text-end'>")
                            .append("<a href='/torneos/editar?id=").append(id).append("' class='btn btn-sm btn-secondary'>Editar</a> ")
                            .append("<a href='/torneos/eliminar?id=").append(id)
                            .append("' onclick='return confirm(\"¿Eliminar torneo?\")' class='btn btn-sm btn-danger'>Eliminar</a>")
                            .append("</div>")
                            .append("</div>")
                            .append("</div>");
                }
            }
            Map<String, Object> model = new HashMap<>();
            model.put("torneos", torneosHtml.toString());
            return new ModelAndView(model, "templates/torneos.vtl");
        }, new VelocityTemplateEngine());

        // 4. Editar torneo - Mostrar formulario pre-rellenado
        get("/torneos/editar", (req, res) -> {
            String idStr = req.queryParams("id");
            if (idStr == null) {
                res.redirect("/torneos");
                return null;
            }
            int id = Integer.parseInt(idStr);
            Map<String, Object> model = new HashMap<>();
            try (Connection conn = DatabaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM torneos WHERE id = ?")) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    // Crear un mapa para representar el torneo
                    Map<String, Object> torneo = new HashMap<>();
                    torneo.put("id", rs.getInt("id"));
                    torneo.put("nombre", rs.getString("nombre"));
                    torneo.put("fechaInicio", rs.getDate("fechaInicio").toString());
                    torneo.put("fechaFin", rs.getDate("fechaFin").toString());
                    torneo.put("descripcion", rs.getString("descripcion"));
                    model.put("torneo", torneo);
                } else {
                    res.redirect("/torneos");
                    return null;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                res.redirect("/torneos");
                return null;
            }
            return new ModelAndView(model, "templates/editarTorneo.vtl");
        }, new VelocityTemplateEngine());

        // 5. Procesar edición del torneo (con validación de fechas)
        post("/torneos/editar", (req, res) -> {
            String idStr = req.queryParams("id");
            if (idStr == null) {
                res.redirect("/torneos");
                return null;
            }
            int id = Integer.parseInt(idStr);
            String nombre = req.queryParams("nombre");
            String fechaInicioStr = req.queryParams("fechaInicio");
            String fechaFinStr = req.queryParams("fechaFin");
            String descripcion = req.queryParams("descripcion");

            java.sql.Date fechaInicio = Date.valueOf(fechaInicioStr);
            java.sql.Date fechaFin = Date.valueOf(fechaFinStr);

            // Validar fechas
            if (fechaFin.before(fechaInicio)) {
                res.redirect("/torneos/editar?id=" + id + "&error=fechasInvalidas");
                return null;
            }

            try (Connection conn = DatabaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "UPDATE torneos SET nombre = ?, fechaInicio = ?, fechaFin = ?, descripcion = ? WHERE id = ?")) {
                stmt.setString(1, nombre);
                stmt.setDate(2, fechaInicio);
                stmt.setDate(3, fechaFin);
                stmt.setString(4, descripcion);
                stmt.setInt(5, id);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            res.redirect("/torneos");
            return null;
        });

        // 6. Eliminar torneo
        get("/torneos/eliminar", (req, res) -> {
            String idStr = req.queryParams("id");
            if (idStr != null) {
                int id = Integer.parseInt(idStr);
                try (Connection conn = DatabaseService.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("DELETE FROM torneos WHERE id = ?")) {
                    stmt.setInt(1, id);
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            res.redirect("/torneos");
            return null;
        });

        // =====================================================
        // Inscripción a partidos
        get("/partidos/inscribirse", (req, res) -> {
            String usuario = req.session().attribute("usuario");
            if (usuario == null) {
                // Si no está autenticado, redirige a login con un mensaje
                res.redirect("/login?toast=Debes iniciar sesión para inscribirte&toastType=warning");
                return null;
            }
            String partidoId = req.queryParams("id");
            // Aquí implementar la lógica para inscribir al usuario al partido.
            // Por ejemplo: registrar la inscripción en la base de datos.
            // ...
            res.redirect("/partidos?toast=Inscripción exitosa&toastType=success");
            return null;
        });
        
    }
}
