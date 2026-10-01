package reservo.server;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@WebSocket
public class WebSocketEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEndpoint.class);
    private static WebSocketManager wsManager;
    private Session session;

    public static void setWebSocketManager(WebSocketManager manager) {
        wsManager = manager;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        if (wsManager != null) {
            wsManager.addConnection(this);
        }
        logger.info("WebSocket client connected: {}", session.getRemoteAddress());
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        if (wsManager != null) {
            wsManager.removeConnection(this);
        }
        logger.info("WebSocket client disconnected: {}", reason);
    }

    @OnWebSocketError
    public void onError(Throwable error) {
        logger.error("WebSocket error", error);
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        logger.debug("Received WebSocket message: {}", message);
        // Echo back or handle client messages if needed
    }

    public void sendMessage(String message) throws IOException {
        if (session != null && session.isOpen()) {
            session.getRemote().sendString(message);
        }
    }
}

