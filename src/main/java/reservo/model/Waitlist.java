package reservo.model;

import java.time.Instant;

public class Waitlist {
    private Integer id;
    private Integer userId;
    private Integer timeSlotId;
    private Instant queuedAt;

    public Waitlist() {}

    public Waitlist(Integer id, Integer userId, Integer timeSlotId, Instant queuedAt) {
        this.id = id;
        this.userId = userId;
        this.timeSlotId = timeSlotId;
        this.queuedAt = queuedAt;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getTimeSlotId() { return timeSlotId; }
    public void setTimeSlotId(Integer timeSlotId) { this.timeSlotId = timeSlotId; }

    public Instant getQueuedAt() { return queuedAt; }
    public void setQueuedAt(Instant queuedAt) { this.queuedAt = queuedAt; }
}

