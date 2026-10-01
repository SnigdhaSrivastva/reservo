package reservo.model;

import java.time.Instant;

public class AuditEvent {
    private Integer id;
    private Instant ts;
    private String type;
    private String payloadJson;
    private Integer userId;
    private Integer resourceId;

    public AuditEvent() {}

    public AuditEvent(Integer id, Instant ts, String type, String payloadJson,
                     Integer userId, Integer resourceId) {
        this.id = id;
        this.ts = ts;
        this.type = type;
        this.payloadJson = payloadJson;
        this.userId = userId;
        this.resourceId = resourceId;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Instant getTs() { return ts; }
    public void setTs(Instant ts) { this.ts = ts; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getResourceId() { return resourceId; }
    public void setResourceId(Integer resourceId) { this.resourceId = resourceId; }
}

