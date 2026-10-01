package reservo.model;

import java.time.Instant;

public class Reservation {
    private Integer id;
    private Integer userId;
    private Integer timeSlotId;
    private Integer qty;
    private ReservationStatus status;
    private String requestId;
    private Instant createdAt;
    private Instant cancelledAt;

    public enum ReservationStatus {
        CONFIRMED, CANCELLED
    }

    public Reservation() {}

    public Reservation(Integer id, Integer userId, Integer timeSlotId, Integer qty,
                      ReservationStatus status, String requestId, 
                      Instant createdAt, Instant cancelledAt) {
        this.id = id;
        this.userId = userId;
        this.timeSlotId = timeSlotId;
        this.qty = qty;
        this.status = status;
        this.requestId = requestId;
        this.createdAt = createdAt;
        this.cancelledAt = cancelledAt;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getTimeSlotId() { return timeSlotId; }
    public void setTimeSlotId(Integer timeSlotId) { this.timeSlotId = timeSlotId; }

    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }

    public boolean isConfirmed() {
        return status == ReservationStatus.CONFIRMED;
    }
}

