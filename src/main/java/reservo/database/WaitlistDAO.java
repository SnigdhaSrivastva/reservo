package reservo.database;

import reservo.model.Waitlist;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class WaitlistDAO {
    
    public Waitlist findByUserAndTimeSlot(Integer userId, Integer timeSlotId) throws SQLException {
        String sql = "SELECT id, user_id, time_slot_id, queued_at FROM waitlist WHERE user_id = ? AND time_slot_id = ?";
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

    public List<Waitlist> findByTimeSlotOrdered(Integer timeSlotId) throws SQLException {
        String sql = "SELECT id, user_id, time_slot_id, queued_at FROM waitlist " +
                     "WHERE time_slot_id = ? ORDER BY queued_at ASC";
        List<Waitlist> entries = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, timeSlotId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(mapRow(rs));
                }
            }
        }
        return entries;
    }

    public Waitlist findHeadOfQueue(Integer timeSlotId) throws SQLException {
        String sql = "SELECT id, user_id, time_slot_id, queued_at FROM waitlist " +
                     "WHERE time_slot_id = ? ORDER BY queued_at ASC LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, timeSlotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public Integer getQueuePosition(Integer userId, Integer timeSlotId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM waitlist WHERE time_slot_id = ? AND queued_at <= " +
                     "(SELECT queued_at FROM waitlist WHERE user_id = ? AND time_slot_id = ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, timeSlotId);
            ps.setInt(2, userId);
            ps.setInt(3, timeSlotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return null;
    }

    public Integer create(Waitlist waitlist) throws SQLException {
        String sql = "INSERT INTO waitlist (user_id, time_slot_id, queued_at) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, waitlist.getUserId());
            ps.setInt(2, waitlist.getTimeSlotId());
            ps.setTimestamp(3, Timestamp.from(waitlist.getQueuedAt() != null ? waitlist.getQueuedAt() : Instant.now()));
            
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
        String sql = "DELETE FROM waitlist WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void deleteByUserAndTimeSlot(Integer userId, Integer timeSlotId) throws SQLException {
        String sql = "DELETE FROM waitlist WHERE user_id = ? AND time_slot_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, timeSlotId);
            ps.executeUpdate();
        }
    }

    private Waitlist mapRow(ResultSet rs) throws SQLException {
        Waitlist waitlist = new Waitlist();
        waitlist.setId(rs.getInt("id"));
        waitlist.setUserId(rs.getInt("user_id"));
        waitlist.setTimeSlotId(rs.getInt("time_slot_id"));
        Timestamp ts = rs.getTimestamp("queued_at");
        waitlist.setQueuedAt(ts != null ? ts.toInstant() : null);
        return waitlist;
    }
}

