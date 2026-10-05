package ru.itmo.web;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@ServerEndpoint("/changes")
public class ChangesEndpoint {
    private static final Logger LOGGER = Logger.getLogger(ChangesEndpoint.class.getName());
    private static final Set<Session> SESSIONS = ConcurrentHashMap.newKeySet();

    @OnOpen
    public void open(Session session) {
        session.getAsyncRemote().setSendTimeout(5000);
        SESSIONS.add(session);
    }

    @OnClose
    public void close(Session session) {
        SESSIONS.remove(session);
    }

    @OnError
    public void error(Session session, Throwable error) {
        if (session != null) {
            discard(session);
        }
        LOGGER.log(Level.FINE, "Соединение обновлений закрыто", error);
    }

    // Called only after the service's container-managed transaction has completed.
    public static void publish() {
        for (Session session : SESSIONS) {
            if (!session.isOpen()) {
                SESSIONS.remove(session);
                continue;
            }
            try {
                session.getAsyncRemote().sendText("{\"type\":\"changed\"}", result -> {
                    if (!result.isOK()) {
                        discard(session);
                    }
                });
            } catch (RuntimeException error) {
                discard(session);
                LOGGER.log(Level.FINE, "Не удалось отправить обновление", error);
            }
        }
    }

    private static void discard(Session session) {
        SESSIONS.remove(session);
        try {
            session.close();
        } catch (IOException | RuntimeException error) {
            LOGGER.log(Level.FINE, "Не удалось закрыть соединение", error);
        }
    }
}
