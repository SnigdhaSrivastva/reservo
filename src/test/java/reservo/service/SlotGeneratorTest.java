package reservo.service;

import org.junit.jupiter.api.Test;
import reservo.model.Resource;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SlotGeneratorTest {
    private static final ZoneId NYC = ZoneId.of("America/New_York");
    private final SlotGenerator generator =
            new SlotGenerator(null, null, NYC, LocalTime.of(8, 0), LocalTime.of(22, 0));

    private static Resource resource(int slotMinutes, int horizonDays) {
        Resource r = new Resource();
        r.setSlotDurationMinutes(slotMinutes);
        r.setBookingHorizonDays(horizonDays);
        return r;
    }

    @Test
    void fillsOpeningHoursWithBackToBackSlots() {
        Instant beforeOpening = ZonedDateTime.of(2026, 3, 2, 6, 0, 0, 0, NYC).toInstant();
        List<Instant[]> slots = generator.slotsFor(resource(60, 1), beforeOpening);

        assertEquals(14, slots.size()); // 08:00 - 22:00
        assertEquals(LocalTime.of(8, 0), slots.get(0)[0].atZone(NYC).toLocalTime());
        assertEquals(LocalTime.of(22, 0), slots.get(13)[1].atZone(NYC).toLocalTime());
        for (int i = 1; i < slots.size(); i++) {
            assertEquals(slots.get(i - 1)[1], slots.get(i)[0], "slots should be contiguous");
        }
    }

    @Test
    void doesNotCreateSlotsThatHaveAlreadyEnded() {
        Instant midday = ZonedDateTime.of(2026, 3, 2, 12, 30, 0, 0, NYC).toInstant();
        List<Instant[]> slots = generator.slotsFor(resource(60, 1), midday);

        assertTrue(slots.stream().allMatch(s -> s[1].isAfter(midday)));
        assertEquals(LocalTime.of(12, 0), slots.get(0)[0].atZone(NYC).toLocalTime());
    }

    @Test
    void dropsPartialSlotAtClosingTime() {
        Instant beforeOpening = ZonedDateTime.of(2026, 3, 2, 6, 0, 0, 0, NYC).toInstant();
        List<Instant[]> slots = generator.slotsFor(resource(90, 1), beforeOpening);

        assertEquals(9, slots.size()); // 14h / 1.5h = 9 full slots, no partial one
        slots.forEach(s -> assertEquals(Duration.ofMinutes(90), Duration.between(s[0], s[1])));
    }

    @Test
    void coversTheWholeBookingHorizon() {
        Instant beforeOpening = ZonedDateTime.of(2026, 3, 2, 6, 0, 0, 0, NYC).toInstant();
        assertEquals(14 * 7, generator.slotsFor(resource(60, 7), beforeOpening).size());
    }
}
