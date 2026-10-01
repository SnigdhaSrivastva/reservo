package reservo.server;

import reservo.database.NotificationDAO;
import reservo.service.AuthService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class NotificationServlet extends BaseServlet {
    private final NotificationDAO notificationDAO;
    private final AuthService authService;

    public NotificationServlet(NotificationDAO notificationDAO, AuthService authService) {
        this.notificationDAO = notificationDAO;
        this.authService = authService;
    }

    private Integer getUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            AuthService.UserSession session = authService.validateSession(token);
            if (session != null) {
                return session.getUser().getId();
            }
        }
        return null;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        Integer userId = getUserId(request);
        if (userId == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated");
            return;
        }

        try {
            boolean unreadOnly = "true".equals(request.getParameter("unreadOnly"));
            sendJsonResponse(response, notificationDAO.findByUserId(userId, unreadOnly));
        } catch (SQLException e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}

