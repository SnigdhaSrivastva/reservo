package reservo.server;

import reservo.database.ResourceDAO;
import reservo.database.DatabaseManager;
import reservo.service.AuthService;
import reservo.service.BookingService;
import com.fasterxml.jackson.databind.JsonNode;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class AdminServlet extends BaseServlet {
    private final ResourceDAO resourceDAO;
    private final BookingService bookingService;
    private final AuthService authService;

    public AdminServlet(ResourceDAO resourceDAO, BookingService bookingService, AuthService authService) {
        this.resourceDAO = resourceDAO;
        this.bookingService = bookingService;
        this.authService = authService;
    }

    private boolean isAdmin(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            AuthService.UserSession session = authService.validateSession(token);
            return session != null && session.getUser().isAdmin();
        }
        return false;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        if (!isAdmin(request)) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return;
        }

        String path = request.getPathInfo();
        if (path == null) path = "";

        try {
            if (path.contains("/isolation-mode")) {
                JsonNode json = objectMapper.readTree(request.getInputStream());
                String level = json.get("level").asText();
                DatabaseManager.setIsolationLevel(level);
                sendJsonResponse(response, new ResultMessage("Isolation level set to " + level));
            } else {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private static class ResultMessage {
        public String message;
        public ResultMessage(String message) { this.message = message; }
    }
}

