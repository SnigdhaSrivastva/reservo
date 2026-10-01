package reservo.database;

import reservo.model.Hold;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class HoldDAO {
    
    public Hold findById(Integer id) throws SQLException {
        String sql = "SELECT id, user_id, time_slot_id, qty, expires_at, request_id, created_at FROM hold WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public Hold findByRequestId(String requestId) throws SQLException {
        String sql = "SELECT id, user_id, time_slot_id, qty, expires_at, request_id, created_at FROM hold WHERE request_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public Hold findByUserAndTimeSlot(Integer userId, Integer timeSlotId) throws SQLException {
        String sql = "SELECT id, user_id, time_slot_id, qty, expires_at, request_id, created_at " +
                     "FROM hold WHERE user_id = ? AND time_slot_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, timeSlotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Hold> findExpiredHolds(Instant before) throws SQLException {
        String sql = "SELECT id, user_id, time_slot_id, qty, expires_at, request_id, created_at " +
                     "FROM hold WHERE expires_at < ?";
        List<Hold> holds = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(before));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    holds.add(mapRow(rs));
                }
            }
        }
        return holds;
    }

    public Integer create(Hold hold) throws SQLException {
        String sql = "INSERT INTO hold (user_id, time_slot_id, qty, expires_at, request_id, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, hold.getUserId());
            ps.setInt(2, hold.getTimeSlotId());
            ps.setInt(3, hold.getQty());
            ps.setTimestamp(4, Timestamp.from(hold.getExpiresAt()));
            ps.setString(5, hold.getRequestId());
            ps.setTimestamp(6, Timestamp.from(hold.getCreatedAt() != null ? hold.getCreatedAt() : Instant.now()));
            
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return null;
    }

    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM hold WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void deleteByTimeSlot(Integer timeSlotId) throws SQLException {
        String sql = "DELETE FROM hold WHERE time_slot_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, timeSlotId);
            ps.executeUpdate();
        }
    }

    public void deleteExpired(Instant before) throws SQLException {
        String sql = "DELETE FROM hold WHERE expires_at < ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(before));
            ps.executeUpdate();
        }
    }

    private Hold mapRow(ResultSet rs) throws SQLException {
        Hold hold = new Hold();
        hold.setId(rs.getInt("id"));
        hold.setUserId(rs.getInt("user_id"));
        hold.setTimeSlotId(rs.getInt("time_slot_id"));
        hold.setQty(rs.getInt("qty"));
        Timestamp ts = rs.getTimestamp("expires_at");
        hold.setExpiresAt(ts != null ? ts.toInstant() : null);
        hold.setRequestId(rs.getString("request_id"));
        ts = rs.getTimestamp("created_at");
        hold.setCreatedAt(ts != null ? ts.toInstant() : null);
        return hold;
    }
}

