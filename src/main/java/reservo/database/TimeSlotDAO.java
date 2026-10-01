package reservo.database;

import reservo.model.TimeSlot;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TimeSlotDAO {
    
    public List<TimeSlot> findByResourceAndDateRange(Integer resourceId, Instant from, Instant to) throws SQLException {
        String sql = "SELECT id, resource_id, start_ts, end_ts, capacity_remaining, version " +
                     "FROM time_slot WHERE resource_id = ? AND start_ts >= ? AND start_ts < ? ORDER BY start_ts";
        List<TimeSlot> slots = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            ps.setTimestamp(2, Timestamp.from(from));
            ps.setTimestamp(3, Timestamp.from(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    slots.add(mapRow(rs));
                }
            }
        }
        return slots;
    }

    public TimeSlot findById(Integer id) throws SQLException {
        String sql = "SELECT id, resource_id, start_ts, end_ts, capacity_remaining, version FROM time_slot WHERE id = ?";
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

    public TimeSlot findByIdForUpdate(Integer id) throws SQLException {
        String sql = "SELECT id, resource_id, start_ts, end_ts, capacity_remaining, version " +
                     "FROM time_slot WHERE id = ? FOR UPDATE";
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

    public void decrementCapacity(Integer id, Integer amount) throws SQLException {
        String sql = "UPDATE time_slot SET capacity_remaining = capacity_remaining - ?, version = version + 1 " +
                     "WHERE id = ? AND capacity_remaining >= ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setInt(2, id);
            ps.setInt(3, amount);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new SQLException("Failed to decrement capacity - insufficient capacity or slot not found");
            }
        }
    }

    public void incrementCapacity(Integer id, Integer amount) throws SQLException {
        String sql = "UPDATE time_slot SET capacity_remaining = capacity_remaining + ?, version = version + 1 WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public List<TimeSlot> findAvailableSlots(Integer resourceId, Instant from, Instant to) throws SQLException {
        String sql = "SELECT id, resource_id, start_ts, end_ts, capacity_remaining, version " +
                     "FROM time_slot WHERE resource_id = ? AND start_ts >= ? AND start_ts < ? " +
                     "AND capacity_remaining > 0 ORDER BY start_ts";
        List<TimeSlot> slots = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            ps.setTimestamp(2, Timestamp.from(from));
            ps.setTimestamp(3, Timestamp.from(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    slots.add(mapRow(rs));
                }
            }
        }
        return slots;
    }

    private TimeSlot mapRow(ResultSet rs) throws SQLException {
        TimeSlot slot = new TimeSlot();
        slot.setId(rs.getInt("id"));
        slot.setResourceId(rs.getInt("resource_id"));
        Timestamp ts = rs.getTimestamp("start_ts");
        slot.setStartTs(ts != null ? ts.toInstant() : null);
        ts = rs.getTimestamp("end_ts");
        slot.setEndTs(ts != null ? ts.toInstant() : null);
        slot.setCapacityRemaining(rs.getInt("capacity_remaining"));
        slot.setVersion(rs.getInt("version"));
        return slot;
    }
}

