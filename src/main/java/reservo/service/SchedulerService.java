package reservo.service;

import reservo.database.*;
import reservo.model.*;
import reservo.service.BookingService;
import reservo.common.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Background scheduler for hold expiry and waitlist promotion.
 */
public class SchedulerService {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);
    private final HoldDAO holdDAO;
    private final WaitlistDAO waitlistDAO;
    private final TimeSlotDAO timeSlotDAO;
    private final ReservationDAO reservationDAO;
    private final NotificationDAO notificationDAO;
    private final AuditDAO auditDAO;
    private final BookingService bookingService;
    private final ScheduledExecutorService executor;
    private final int cleanupIntervalSeconds;
    private final int promotionIntervalSeconds;
    private Consumer<Event> eventPublisher;

    public interface Event {
        String getType();
    }

    public SchedulerService(HoldDAO holdDAO, WaitlistDAO waitlistDAO, TimeSlotDAO timeSlotDAO,
                           ReservationDAO reservationDAO, NotificationDAO notificationDAO,
                           AuditDAO auditDAO, BookingService bookingService,
                           int cleanupIntervalSeconds, int promotionIntervalSeconds) {
        this.holdDAO = holdDAO;
        this.waitlistDAO = waitlistDAO;
        this.timeSlotDAO = timeSlotDAO;
        this.reservationDAO = reservationDAO;
        this.notificationDAO = notificationDAO;
        this.auditDAO = auditDAO;
        this.bookingService = bookingService;
        this.cleanupIntervalSeconds = cleanupIntervalSeconds;
        this.promotionIntervalSeconds = promotionIntervalSeconds;
        this.executor = Executors.newScheduledThreadPool(2);
    }

    public void start() {
        logger.info("Starting scheduler service...");
        
        // Schedule hold cleanup
        executor.scheduleWithFixedDelay(this::cleanupExpiredHolds, 
                                       cleanupIntervalSeconds, 
                                       cleanupIntervalSeconds, 
                                       TimeUnit.SECONDS);

        // Schedule waitlist promotion
        executor.scheduleWithFixedDelay(this::promoteWaitlist, 
                                       promotionIntervalSeconds, 
                                       promotionIntervalSeconds, 
                                       TimeUnit.SECONDS);
    }

    public void stop() {
        logger.info("Stopping scheduler service...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void setEventPublisher(Consumer<Event> publisher) {
        this.eventPublisher = publisher;
    }

    private void cleanupExpiredHolds() {
        try {
            Instant now = Instant.now();
            List<Hold> expiredHolds = holdDAO.findExpiredHolds(now);

            for (Hold hold : expiredHolds) {
                try (Connection conn = DatabaseManager.getConnection()) {
                    conn.setAutoCommit(false);
                    try {
                        TimeSlot slot = timeSlotDAO.findById(hold.getTimeSlotId());
                        
                        if (!holdDAO.delete(hold.getId())) {
                            continue; // already confirmed or cancelled
                        }
                        timeSlotDAO.incrementCapacity(hold.getTimeSlotId(), hold.getQty());

                        if (slot != null) {
                            audit("HOLD_EXPIRED", hold.getUserId(), slot.getResourceId(),
                                  String.format("{\"holdId\":%d,\"timeSlotId\":%d}", hold.getId(), hold.getTimeSlotId()));
                        }

                        conn.commit();

                        publishEvent(new AvailabilityChangedEvent(hold.getTimeSlotId()));

                    } catch (Exception e) {
                        conn.rollback();
                        logger.error("Error cleaning up hold {}", hold.getId(), e);
                    }
                }
            }

            if (!expiredHolds.isEmpty()) {
                logger.debug("Cleaned up {} expired holds", expiredHolds.size());
            }

        } catch (SQLException e) {
            logger.error("Error during hold cleanup", e);
        }
    }

    private void promoteWaitlist() {
        try {
            // Find all time slots with available capacity and waitlist entries
            // For simplicity, we'll check a subset. In production, use a more efficient query.
            
            // Get all unique time slots with waitlist entries that have capacity
            List<TimeSlot> slotsWithCapacity = timeSlotDAO.findAvailableSlots(1, Instant.now(), Instant.now().plus(30, ChronoUnit.DAYS));
            
            for (TimeSlot slot : slotsWithCapacity) {
                try {
                    promoteWaitlistForSlot(slot.getId());
                } catch (Exception e) {
                    logger.error("Error promoting waitlist for slot {}", slot.getId(), e);
                }
            }

        } catch (SQLException e) {
            logger.error("Error during waitlist promotion", e);
        }
    }

    private void promoteWaitlistForSlot(Integer timeSlotId) throws SQLException {
        TimeSlot slot = timeSlotDAO.findByIdForUpdate(timeSlotId);
        if (slot == null || slot.getCapacityRemaining() <= 0) {
            return;
        }

        List<Waitlist> queue = waitlistDAO.findByTimeSlotOrdered(timeSlotId);
        if (queue.isEmpty()) {
            return;
        }

        // Promote up to capacity_remaining entries
        int promotions = Math.min(slot.getCapacityRemaining(), queue.size());
        
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (int i = 0; i < promotions; i++) {
                    Waitlist next = queue.get(i);
                    
                    // Create reservation
                    Reservation reservation = new Reservation();
                    reservation.setUserId(next.getUserId());
                    reservation.setTimeSlotId(timeSlotId);
                    reservation.setQty(1); // Default quantity
                    reservation.setStatus(Reservation.ReservationStatus.CONFIRMED);
                    reservation.setRequestId(PasswordUtil.generateRequestId());
                    reservation.setCreatedAt(Instant.now());

                    Integer reservationId = reservationDAO.create(reservation);
                    reservation.setId(reservationId);

                    // Decrement capacity
                    timeSlotDAO.decrementCapacity(timeSlotId, 1);

                    // Remove from waitlist
                    waitlistDAO.delete(next.getId());

                    // Create notification
                    Notification notification = new Notification();
                    notification.setUserId(next.getUserId());
                    notification.setType("PROMOTED");
                    notification.setTitle("Reservation Available!");
                    notification.setMessage(String.format("You've been promoted from the waitlist. Your reservation is confirmed."));
                    notification.setRead(false);
                    notification.setCreatedAt(Instant.now());
                    notificationDAO.create(notification);

                    audit("WAITLIST_PROMOTED", next.getUserId(), slot.getResourceId(),
                          String.format("{\"reservationId\":%d,\"timeSlotId\":%d}", reservationId, timeSlotId));

                    publishEvent(new PromotedEvent(next.getUserId(), timeSlotId, reservationId));
                }

                conn.commit();
                
                if (promotions > 0) {
                    logger.info("Promoted {} users from waitlist for slot {}", promotions, timeSlotId);
                    publishEvent(new AvailabilityChangedEvent(timeSlotId));
                }

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private void audit(String type, Integer userId, Integer resourceId, String payload) {
        try {
            AuditEvent event = new AuditEvent();
            event.setType(type);
            event.setUserId(userId);
            event.setResourceId(resourceId);
            event.setPayloadJson(payload);
            event.setTs(Instant.now());
            auditDAO.create(event);
        } catch (SQLException e) {
            logger.error("Failed to write audit event", e);
        }
    }

    private void publishEvent(Event event) {
        if (eventPublisher != null) {
            try {
                eventPublisher.accept(event);
            } catch (Exception e) {
                logger.error("Error publishing event", e);
            }
        }
    }

    // Event classes
    public static class AvailabilityChangedEvent implements Event {
        private final Integer timeSlotId;

        public AvailabilityChangedEvent(Integer timeSlotId) {
            this.timeSlotId = timeSlotId;
        }

        @Override
        public String getType() { return "AVAILABILITY_CHANGED"; }
        public Integer getTimeSlotId() { return timeSlotId; }
    }

    public static class PromotedEvent implements Event {
        private final Integer userId;
        private final Integer timeSlotId;
        private final Integer reservationId;

        public PromotedEvent(Integer userId, Integer timeSlotId, Integer reservationId) {
            this.userId = userId;
            this.timeSlotId = timeSlotId;
            this.reservationId = reservationId;
        }

        @Override
        public String getType() { return "PROMOTED"; }
        public Integer getUserId() { return userId; }
        public Integer getTimeSlotId() { return timeSlotId; }
        public Integer getReservationId() { return reservationId; }
    }
}

