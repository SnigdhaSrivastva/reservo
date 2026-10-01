package reservo.service;

import reservo.database.ResourceDAO;
import reservo.database.TimeSlotDAO;
import reservo.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Materializes bookable time slots for every resource across its booking horizon.
 * Idempotent: existing slots are left untouched, so it can run on every startup.
 */
public class SlotGenerator {
    private static final Logger logger = LoggerFactory.getLogger(SlotGenerator.class);

    private final ResourceDAO resourceDAO;
    private final TimeSlotDAO timeSlotDAO;
    private final ZoneId zone;
    private final LocalTime openTime;
    private final LocalTime closeTime;

    public SlotGenerator(ResourceDAO resourceDAO, TimeSlotDAO timeSlotDAO,
                         ZoneId zone, LocalTime openTime, LocalTime closeTime) {
        this.resourceDAO = resourceDAO;
        this.timeSlotDAO = timeSlotDAO;
        this.zone = zone;
        this.openTime = openTime;
        this.closeTime = closeTime;
    }

    public int generate(Instant now) {
        int total = 0;
        try {
            for (Resource resource : resourceDAO.findAll()) {
                List<Instant[]> slots = slotsFor(resource, now);
                total += timeSlotDAO.insertMissingSlots(resource.getId(), resource.getCapacity(), slots);
            }
            logger.info("Slot generation complete: {} new slots", total);
        } catch (Exception e) {
            logger.error("Slot generation failed", e);
        }
        return total;
    }

    /** Builds [start, end) pairs within opening hours, from today through the resource's booking horizon. */
    List<Instant[]> slotsFor(Resource resource, Instant now) {
        int duration = resource.getSlotDurationMinutes();
        LocalDate today = LocalDate.ofInstant(now, zone);
        List<Instant[]> slots = new ArrayList<>();

        for (int day = 0; day < resource.getBookingHorizonDays(); day++) {
            LocalDate date = today.plusDays(day);
            ZonedDateTime start = date.atTime(openTime).atZone(zone);
            ZonedDateTime close = date.atTime(closeTime).atZone(zone);
            while (!start.plusMinutes(duration).isAfter(close)) {
                ZonedDateTime end = start.plusMinutes(duration);
                if (end.toInstant().isAfter(now)) {
                    slots.add(new Instant[] {start.toInstant(), end.toInstant()});
                }
                start = end;
            }
        }
        return slots;
    }
}
