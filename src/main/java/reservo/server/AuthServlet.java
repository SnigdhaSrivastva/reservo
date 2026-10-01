package reservo.server;

import reservo.service.AuthService;
import com.fasterxml.jackson.databind.JsonNode;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthServlet extends BaseServlet {
    private final AuthService authService;

    public AuthServlet(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String path = request.getPathInfo();
        if (path == null) path = "";

        try {
            JsonNode json = objectMapper.readTree(request.getInputStream());

            if (path.equals("/login") || path.endsWith("/login")) {
                String username = json.get("username").asText();
                String password = json.get("password").asText();
                
                AuthService.UserSession session = authService.login(username, password);
                sendJsonResponse(response, new LoginResponse(session.getToken(), session.getUser()));
                
            } else if (path.equals("/register") || path.endsWith("/register")) {
                String username = json.get("username").asText();
                String email = json.get("email").asText();
                String password = json.get("password").asText();
                
                AuthService.UserSession session = authService.register(username, email, password);
                sendJsonResponse(response, new LoginResponse(session.getToken(), session.getUser()));
                
            } else {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private static class LoginResponse {
        public String token;
        public UserDto user;

        public LoginResponse(String token, reservo.model.User user) {
            this.token = token;
            this.user = new UserDto(user);
        }
    }

    private static class UserDto {
        public Integer id;
        public String username;
        public String email;
        public String role;

        public UserDto(reservo.model.User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.email = user.getEmail();
            this.role = user.getRole().name();
        }
    }
}

