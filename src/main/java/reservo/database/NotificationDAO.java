package reservo.database;

import reservo.model.Notification;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {
    
    public List<Notification> findByUserId(Integer userId, boolean unreadOnly) throws SQLException {
        String sql = "SELECT id, user_id, type, title, message, read, created_at FROM notification " +
                     "WHERE user_id = ?";
        if (unreadOnly) {
            sql += " AND read = FALSE";
        }
        sql += " ORDER BY created_at DESC";
        
        List<Notification> notifications = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapRow(rs));
                }
            }
        }
        return notifications;
    }

    public Integer create(Notification notification) throws SQLException {
        String sql = "INSERT INTO notification (user_id, type, title, message, read, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, notification.getUserId());
            ps.setString(2, notification.getType());
            ps.setString(3, notification.getTitle());
            ps.setString(4, notification.getMessage());
            ps.setBoolean(5, notification.getRead() != null ? notification.getRead() : false);
            ps.setTimestamp(6, Timestamp.from(notification.getCreatedAt() != null ? notification.getCreatedAt() : Instant.now()));
            
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return null;
    }

    public void markAsRead(Integer id) throws SQLException {
        String sql = "UPDATE notification SET read = TRUE WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification notification = new Notification();
        notification.setId(rs.getInt("id"));
        notification.setUserId(rs.getInt("user_id"));
        notification.setType(rs.getString("type"));
        notification.setTitle(rs.getString("title"));
        notification.setMessage(rs.getString("message"));
        notification.setRead(rs.getBoolean("read"));
        Timestamp ts = rs.getTimestamp("created_at");
        notification.setCreatedAt(ts != null ? ts.toInstant() : null);
        return notification;
    }
}

