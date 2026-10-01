package reservo.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reservo.database.*;
import reservo.model.Hold;
import reservo.model.Resource;
import reservo.model.TimeSlot;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the core guarantee: when many users race for the same slot,
 * capacity is never oversold and each hold is consumed at most once.
 */
class BookingConcurrencyTest {
    private static final int THREADS = 24;

    private static final TimeSlotDAO timeSlotDAO = new TimeSlotDAO();
    private static final HoldDAO holdDAO = new HoldDAO();
    private static final ReservationDAO reservationDAO = new ReservationDAO();
    private static final ResourceDAO resourceDAO = new ResourceDAO();
    private static AuthService authService;
    private static BookingService bookingService;

    private final List<Integer> userIds = new ArrayList<>();

    @BeforeAll
    static void setUpDatabase() {
        DatabaseManager.initialize();
        MigrationRunner.runMigrations();
        authService = new AuthService(new UserDAO());
        bookingService = new BookingService(timeSlotDAO, holdDAO, reservationDAO, new WaitlistDAO(),
                new NotificationDAO(), new AuditDAO(), resourceDAO, 300);
    }

    @BeforeEach
    void createUsers() throws Exception {
        for (int i = 0; i < THREADS; i++) {
            String name = "user-" + UUID.randomUUID().toString().substring(0, 8);
            userIds.add(authService.register(name, name + "@test.local", "password123").getUser().getId());
        }
    }

    @Test
    void onlyOneUserCanHoldTheLastUnit() throws Exception {
        int slotId = createSlot(1);

        AtomicInteger successes = new AtomicInteger();
        AtomicInteger rejections = new AtomicInteger();
        runConcurrently(THREADS, i -> {
            try {
                bookingService.createHold(userIds.get(i), slotId, 1, "hold-" + UUID.randomUUID());
                successes.incrementAndGet();
            } catch (IllegalStateException e) {
                rejections.incrementAndGet();
            }
        });

        assertEquals(1, successes.get(), "exactly one hold should win");
        assertEquals(THREADS - 1, rejections.get());
        assertEquals(0, timeSlotDAO.findById(slotId).getCapacityRemaining());
    }

    @Test
    void capacityIsNeverOversold() throws Exception {
        int capacity = 5;
        int slotId = createSlot(capacity);

        AtomicInteger successes = new AtomicInteger();
        runConcurrently(THREADS, i -> {
            try {
                bookingService.createHold(userIds.get(i), slotId, 1, "hold-" + UUID.randomUUID());
                successes.incrementAndGet();
            } catch (IllegalStateException ignored) {
                // sold out
            }
        });

        assertEquals(capacity, successes.get());
        assertEquals(0, timeSlotDAO.findById(slotId).getCapacityRemaining());
    }

    @Test
    void aHoldCanBeConfirmedOnlyOnce() throws Exception {
        int slotId = createSlot(1);
        int owner = userIds.get(0);
        Hold hold = bookingService.createHold(owner, slotId, 1, "hold-" + UUID.randomUUID());

        AtomicInteger confirmed = new AtomicInteger();
        runConcurrently(8, i -> {
            try {
                bookingService.confirmHold(hold.getId(), owner, "confirm-" + UUID.randomUUID());
                confirmed.incrementAndGet();
            } catch (IllegalStateException | IllegalArgumentException ignored) {
                // hold already consumed
            }
        });

        assertEquals(1, confirmed.get(), "a hold must turn into exactly one reservation");
        assertEquals(1, reservationDAO.findConfirmedByTimeSlot(slotId).size());
        assertEquals(0, timeSlotDAO.findById(slotId).getCapacityRemaining());
    }

    @Test
    void cancellingAHoldReleasesCapacity() throws Exception {
        int slotId = createSlot(1);
        int owner = userIds.get(0);
        Hold hold = bookingService.createHold(owner, slotId, 1, "hold-" + UUID.randomUUID());
        assertEquals(0, timeSlotDAO.findById(slotId).getCapacityRemaining());

        bookingService.cancelHold(hold.getId(), owner);

        assertEquals(1, timeSlotDAO.findById(slotId).getCapacityRemaining());
        assertDoesNotThrow(() -> bookingService.createHold(userIds.get(1), slotId, 1, "hold-" + UUID.randomUUID()));
    }

    private int createSlot(int capacity) throws Exception {
        Resource resource = new Resource();
        resource.setName("Test Room " + UUID.randomUUID());
        resource.setDescription("test");
        resource.setCapacity(capacity);
        resource.setSlotDurationMinutes(60);
        resource.setBookingHorizonDays(1);
        resource.setMaxBookingHoursPerDay(24.0);
        resource.setRulesJson("{}");
        int resourceId = resourceDAO.create(resource);

        Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(1, ChronoUnit.HOURS);
        timeSlotDAO.insertMissingSlots(resourceId, capacity, List.<Instant[]>of(new Instant[] {start, end}));

        List<TimeSlot> slots = timeSlotDAO.findByResourceAndDateRange(resourceId, start, end);
        assertEquals(1, slots.size());
        return slots.get(0).getId();
    }

    @FunctionalInterface
    interface Task {
        void run(int index) throws Exception;
    }

    /** Releases all workers at once via a latch to maximize contention. */
    private static void runConcurrently(int workers, Task task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < workers; i++) {
            int index = i;
            futures.add(pool.submit(() -> {
                start.await();
                task.run(index);
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();
    }
}
