package reservo.model;

import java.time.Instant;

public class TimeSlot {
    private Integer id;
    private Integer resourceId;
    private Instant startTs;
    private Instant endTs;
    private Integer capacityRemaining;
    private Integer version;

    public TimeSlot() {}

    public TimeSlot(Integer id, Integer resourceId, Instant startTs, Instant endTs, 
                   Integer capacityRemaining, Integer version) {
        this.id = id;
        this.resourceId = resourceId;
        this.startTs = startTs;
        this.endTs = endTs;
        this.capacityRemaining = capacityRemaining;
        this.version = version;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getResourceId() { return resourceId; }
    public void setResourceId(Integer resourceId) { this.resourceId = resourceId; }

    public Instant getStartTs() { return startTs; }
    public void setStartTs(Instant startTs) { this.startTs = startTs; }

    public Instant getEndTs() { return endTs; }
    public void setEndTs(Instant endTs) { this.endTs = endTs; }

    public Integer getCapacityRemaining() { return capacityRemaining; }
    public void setCapacityRemaining(Integer capacityRemaining) { this.capacityRemaining = capacityRemaining; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public boolean isAvailable() {
        return capacityRemaining != null && capacityRemaining > 0;
    }
}

