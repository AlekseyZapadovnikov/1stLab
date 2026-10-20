package ru.itmo.web;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/** Включает JAX-RS и задаёт общий префикс /api для HTTP-ресурсов приложения. */
@ApplicationPath("/api")
public class RestApplication extends Application {
}
