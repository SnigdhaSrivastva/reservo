package reservo.database;

import reservo.model.Resource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ResourceDAO {
    
    public List<Resource> findAll() throws SQLException {
        String sql = "SELECT id, name, description, capacity, slot_duration_minutes, booking_horizon_days, " +
                     "max_booking_hours_per_day, rules_json, created_at, updated_at FROM resources ORDER BY name";
        List<Resource> resources = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                resources.add(mapRow(rs));
            }
        }
        return resources;
    }

    public Resource findById(Integer id) throws SQLException {
        String sql = "SELECT id, name, description, capacity, slot_duration_minutes, booking_horizon_days, " +
                     "max_booking_hours_per_day, rules_json, created_at, updated_at FROM resources WHERE id = ?";
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

    public Integer create(Resource resource) throws SQLException {
        String sql = "INSERT INTO resources (name, description, capacity, slot_duration_minutes, " +
                     "booking_horizon_days, max_booking_hours_per_day, rules_json, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Instant now = Instant.now();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, resource.getName());
            ps.setString(2, resource.getDescription());
            ps.setInt(3, resource.getCapacity());
            ps.setInt(4, resource.getSlotDurationMinutes());
            ps.setInt(5, resource.getBookingHorizonDays());
            if (resource.getMaxBookingHoursPerDay() != null) {
                ps.setDouble(6, resource.getMaxBookingHoursPerDay());
            } else {
                ps.setNull(6, Types.DOUBLE);
            }
            ps.setString(7, resource.getRulesJson());
            ps.setTimestamp(8, Timestamp.from(resource.getCreatedAt() != null ? resource.getCreatedAt() : now));
            ps.setTimestamp(9, Timestamp.from(resource.getUpdatedAt() != null ? resource.getUpdatedAt() : now));
            
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return null;
    }

    public void update(Resource resource) throws SQLException {
        String sql = "UPDATE resources SET name = ?, description = ?, capacity = ?, slot_duration_minutes = ?, " +
                     "booking_horizon_days = ?, max_booking_hours_per_day = ?, rules_json = ?, updated_at = ? " +
                     "WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resource.getName());
            ps.setString(2, resource.getDescription());
            ps.setInt(3, resource.getCapacity());
            ps.setInt(4, resource.getSlotDurationMinutes());
            ps.setInt(5, resource.getBookingHorizonDays());
            if (resource.getMaxBookingHoursPerDay() != null) {
                ps.setDouble(6, resource.getMaxBookingHoursPerDay());
            } else {
                ps.setNull(6, Types.DOUBLE);
            }
            ps.setString(7, resource.getRulesJson());
            ps.setTimestamp(8, Timestamp.from(Instant.now()));
            ps.setInt(9, resource.getId());
            
            ps.executeUpdate();
        }
    }

    private Resource mapRow(ResultSet rs) throws SQLException {
        Resource resource = new Resource();
        resource.setId(rs.getInt("id"));
        resource.setName(rs.getString("name"));
        resource.setDescription(rs.getString("description"));
        resource.setCapacity(rs.getInt("capacity"));
        resource.setSlotDurationMinutes(rs.getInt("slot_duration_minutes"));
        resource.setBookingHorizonDays(rs.getInt("booking_horizon_days"));
        double maxHours = rs.getDouble("max_booking_hours_per_day");
        resource.setMaxBookingHoursPerDay(rs.wasNull() ? null : maxHours);
        resource.setRulesJson(rs.getString("rules_json"));
        Timestamp ts = rs.getTimestamp("created_at");
        resource.setCreatedAt(ts != null ? ts.toInstant() : null);
        ts = rs.getTimestamp("updated_at");
        resource.setUpdatedAt(ts != null ? ts.toInstant() : null);
        return resource;
    }
}

