package reservo.model;

import java.time.Instant;

public class Resource {
    private Integer id;
    private String name;
    private String description;
    private Integer capacity;
    private Integer slotDurationMinutes;
    private Integer bookingHorizonDays;
    private Double maxBookingHoursPerDay;
    private String rulesJson;
    private Instant createdAt;
    private Instant updatedAt;

    public Resource() {}

    public Resource(Integer id, String name, String description, Integer capacity, 
                   Integer slotDurationMinutes, Integer bookingHorizonDays,
                   Double maxBookingHoursPerDay, String rulesJson, 
                   Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.capacity = capacity;
        this.slotDurationMinutes = slotDurationMinutes;
        this.bookingHorizonDays = bookingHorizonDays;
        this.maxBookingHoursPerDay = maxBookingHoursPerDay;
        this.rulesJson = rulesJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Integer getSlotDurationMinutes() { return slotDurationMinutes; }
    public void setSlotDurationMinutes(Integer slotDurationMinutes) { this.slotDurationMinutes = slotDurationMinutes; }

    public Integer getBookingHorizonDays() { return bookingHorizonDays; }
    public void setBookingHorizonDays(Integer bookingHorizonDays) { this.bookingHorizonDays = bookingHorizonDays; }

    public Double getMaxBookingHoursPerDay() { return maxBookingHoursPerDay; }
    public void setMaxBookingHoursPerDay(Double maxBookingHoursPerDay) { this.maxBookingHoursPerDay = maxBookingHoursPerDay; }

    public String getRulesJson() { return rulesJson; }
    public void setRulesJson(String rulesJson) { this.rulesJson = rulesJson; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

