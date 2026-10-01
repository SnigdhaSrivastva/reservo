package reservo.database;

import reservo.model.AuditEvent;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AuditDAO {
    
    public void create(AuditEvent event) throws SQLException {
        String sql = "INSERT INTO audit_event (ts, type, payload_json, user_id, resource_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(event.getTs() != null ? event.getTs() : Instant.now()));
            ps.setString(2, event.getType());
            ps.setString(3, event.getPayloadJson());
            if (event.getUserId() != null) {
                ps.setInt(4, event.getUserId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            if (event.getResourceId() != null) {
                ps.setInt(5, event.getResourceId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.executeUpdate();
        }
    }

    public List<AuditEvent> findByTimeSlot(Integer timeSlotId, Instant from, Instant to) throws SQLException {
        String sql = "SELECT id, ts, type, payload_json, user_id, resource_id FROM audit_event " +
                     "WHERE resource_id = ? AND ts >= ? AND ts < ? ORDER BY ts ASC";
        List<AuditEvent> events = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, timeSlotId);
            ps.setTimestamp(2, Timestamp.from(from));
            ps.setTimestamp(3, Timestamp.from(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(mapRow(rs));
                }
            }
        }
        return events;
    }

    public List<AuditEvent> findByUser(Integer userId, Instant from, Instant to) throws SQLException {
        String sql = "SELECT id, ts, type, payload_json, user_id, resource_id FROM audit_event " +
                     "WHERE user_id = ? AND ts >= ? AND ts < ? ORDER BY ts ASC";
        List<AuditEvent> events = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setTimestamp(2, Timestamp.from(from));
            ps.setTimestamp(3, Timestamp.from(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(mapRow(rs));
                }
            }
        }
        return events;
    }

    private AuditEvent mapRow(ResultSet rs) throws SQLException {
        AuditEvent event = new AuditEvent();
        event.setId(rs.getInt("id"));
        Timestamp ts = rs.getTimestamp("ts");
        event.setTs(ts != null ? ts.toInstant() : null);
        event.setType(rs.getString("type"));
        event.setPayloadJson(rs.getString("payload_json"));
        int userId = rs.getInt("user_id");
        event.setUserId(rs.wasNull() ? null : userId);
        int resourceId = rs.getInt("resource_id");
        event.setResourceId(rs.wasNull() ? null : resourceId);
        return event;
    }
}

