package ru.itmo.web;

import jakarta.json.Json;
import jakarta.json.stream.JsonParsingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger LOGGER = Logger.getLogger(ApiExceptionMapper.class.getName());

    @Override
    public Response toResponse(Exception exception) {
        int status = 500;
        String message = "Не удалось выполнить операцию. Попробуйте ещё раз.";
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof JsonParsingException) {
                status = 400;
                message = "Некорректный JSON в запросе";
                break;
            }
            if (cause instanceof WebApplicationException web) {
                status = web.getResponse().getStatus();
                message = web.getMessage();
                if (web.getCause() != null || message == null || message.startsWith("HTTP ")) {
                    message = switch (status) {
                        case 400 -> "Некорректные параметры или JSON в запросе";
                        case 404 -> "Объект или адрес не найден";
                        case 405 -> "Этот HTTP-метод не поддерживается";
                        case 415 -> "Отправьте данные в формате application/json";
                        default -> "Запрос не удалось выполнить";
                    };
                }
                break;
            }
            if (cause instanceof EntityNotFoundException) {
                status = 404;
                message = "Объект не найден: возможно, он уже удалён другим пользователем";
                break;
            }
            if (cause instanceof IllegalArgumentException) {
                status = 400;
                message = cause.getMessage();
                break;
            }
            if (cause instanceof IllegalStateException) {
                status = 409;
                message = cause.getMessage();
                break;
            }
            if (cause instanceof SQLException sql && "23503".equals(sql.getSQLState())) {
                status = 409;
                message = "Невозможно удалить объект: с ним связаны другие объекты";
                break;
            }
        }
        if (status >= 500) {
            LOGGER.log(Level.SEVERE, "Ошибка API", exception);
        }
        if (message == null || message.isBlank()) {
            message = "Запрос не удалось выполнить";
        }
        return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE)
                .entity(Json.createObjectBuilder().add("message", message).build()).build();
    }
}
