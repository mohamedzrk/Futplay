package com.futplay;

import static spark.Spark.*;
import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // Inicializar la base de datos (se definirá en DatabaseService.java)
        DatabaseService.initializeDatabase();

        // Configurar el puerto del servidor (4567 por defecto)
        port(4567);

        // Definir una ruta básica para la página de inicio
        get("/", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("titulo", "Bienvenido a FutPlay");
            model.put("mensaje", "Organiza partidos y torneos en tu zona");
            return new ModelAndView(model, "templates/index.vtl");
        }, new VelocityTemplateEngine());
    }
}
