package reservo.database;

import reservo.model.Reservation;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {
    
    public Reservation findById(Integer id) throws SQLException {
        String sql = "SELECT id, user_id, time_slot_id, qty, status, request_id, created_at, cancelled_at " +
                     "FROM reservation WHERE id = ?";
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

    public Reservation findByRequestId(String requestId) throws SQLException {
        String sql = "SELECT id, user_id, time_slot_id, qty, status, request_id, created_at, cancelled_at " +
                     "FROM reservation WHERE request_id = ?";
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

    public List<Reservation> findByUserId(Integer userId) throws SQLException {
        String sql = "SELECT id, user_id, time_slot_id, qty, status, request_id, created_at, cancelled_at " +
                     "FROM reservation WHERE user_id = ? ORDER BY created_at DESC";
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        }
        return reservations;
    }

    public List<Reservation> findConfirmedByTimeSlot(Integer timeSlotId) throws SQLException {
        String sql = "SELECT id, user_id, time_slot_id, qty, status, request_id, created_at, cancelled_at " +
                     "FROM reservation WHERE time_slot_id = ? AND status = 'CONFIRMED'";
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, timeSlotId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        }
        return reservations;
    }

    public Integer create(Reservation reservation) throws SQLException {
        String sql = "INSERT INTO reservation (user_id, time_slot_id, qty, status, request_id, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, reservation.getUserId());
            ps.setInt(2, reservation.getTimeSlotId());
            ps.setInt(3, reservation.getQty());
            ps.setString(4, reservation.getStatus().name());
            ps.setString(5, reservation.getRequestId());
            ps.setTimestamp(6, Timestamp.from(reservation.getCreatedAt() != null ? reservation.getCreatedAt() : Instant.now()));
            
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return null;
    }

    public void cancel(Integer id) throws SQLException {
        String sql = "UPDATE reservation SET status = 'CANCELLED', cancelled_at = ? WHERE id = ? AND status = 'CONFIRMED'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(Instant.now()));
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation();
        reservation.setId(rs.getInt("id"));
        reservation.setUserId(rs.getInt("user_id"));
        reservation.setTimeSlotId(rs.getInt("time_slot_id"));
        reservation.setQty(rs.getInt("qty"));
        reservation.setStatus(Reservation.ReservationStatus.valueOf(rs.getString("status")));
        reservation.setRequestId(rs.getString("request_id"));
        Timestamp ts = rs.getTimestamp("created_at");
        reservation.setCreatedAt(ts != null ? ts.toInstant() : null);
        ts = rs.getTimestamp("cancelled_at");
        reservation.setCancelledAt(ts != null ? ts.toInstant() : null);
        return reservation;
    }
}

