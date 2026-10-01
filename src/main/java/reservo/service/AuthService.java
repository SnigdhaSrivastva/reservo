package reservo.service;

import reservo.database.UserDAO;
import reservo.model.User;
import reservo.common.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication service with simple session management.
 */
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserDAO userDAO;
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public UserSession login(String username, String password) throws SQLException {
        User user = userDAO.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = PasswordUtil.generateRequestId();
        UserSession session = new UserSession(token, user, Instant.now().plusSeconds(86400)); // 24 hours
        sessions.put(token, session);
        logger.info("User {} logged in", username);
        return session;
    }

    public UserSession register(String username, String email, String password) throws SQLException {
        if (userDAO.findByUsername(username) != null) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userDAO.findByEmail(email) != null) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(PasswordUtil.hashPassword(password));
        user.setRole(User.UserRole.USER);
        user.setCreatedAt(Instant.now());

        Integer userId = userDAO.create(user);
        user.setId(userId);

        String token = PasswordUtil.generateRequestId();
        UserSession session = new UserSession(token, user, Instant.now().plusSeconds(86400));
        sessions.put(token, session);
        logger.info("User {} registered", username);
        return session;
    }

    public UserSession validateSession(String token) {
        UserSession session = sessions.get(token);
        if (session == null || session.isExpired()) {
            return null;
        }
        return session;
    }

    public void logout(String token) {
        sessions.remove(token);
    }

    public static class UserSession {
        private final String token;
        private final User user;
        private final Instant expiresAt;

        public UserSession(String token, User user, Instant expiresAt) {
            this.token = token;
            this.user = user;
            this.expiresAt = expiresAt;
        }

        public String getToken() { return token; }
        public User getUser() { return user; }
        public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    }
}

