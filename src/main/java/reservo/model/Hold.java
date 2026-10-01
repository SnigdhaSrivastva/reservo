package reservo.model;

import java.time.Instant;

public class Hold {
    private Integer id;
    private Integer userId;
    private Integer timeSlotId;
    private Integer qty;
    private Instant expiresAt;
    private String requestId;
    private Instant createdAt;

    public Hold() {}

    public Hold(Integer id, Integer userId, Integer timeSlotId, Integer qty, 
               Instant expiresAt, String requestId, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.timeSlotId = timeSlotId;
        this.qty = qty;
        this.expiresAt = expiresAt;
        this.requestId = requestId;
        this.createdAt = createdAt;
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

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}

