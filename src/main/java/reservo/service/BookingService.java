package reservo.service;

import reservo.database.*;
import reservo.model.*;
import reservo.common.PasswordUtil;
import reservo.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Core booking service with transaction-safe operations.
 */
public class BookingService {
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);
    private final TimeSlotDAO timeSlotDAO;
    private final HoldDAO holdDAO;
    private final ReservationDAO reservationDAO;
    private final WaitlistDAO waitlistDAO;
    private final NotificationDAO notificationDAO;
    private final AuditDAO auditDAO;
    private final ResourceDAO resourceDAO;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int holdDurationSeconds;
    private java.util.function.Consumer<Event> eventPublisher;

    public interface Event {
        String getType();
    }

    public BookingService(TimeSlotDAO timeSlotDAO, HoldDAO holdDAO, ReservationDAO reservationDAO,
                         WaitlistDAO waitlistDAO, NotificationDAO notificationDAO, AuditDAO auditDAO,
                         ResourceDAO resourceDAO, int holdDurationSeconds) {
        this.timeSlotDAO = timeSlotDAO;
        this.holdDAO = holdDAO;
        this.reservationDAO = reservationDAO;
        this.waitlistDAO = waitlistDAO;
        this.notificationDAO = notificationDAO;
        this.auditDAO = auditDAO;
        this.resourceDAO = resourceDAO;
        this.holdDurationSeconds = holdDurationSeconds;
    }

    public void setEventPublisher(java.util.function.Consumer<Event> publisher) {
        this.eventPublisher = publisher;
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

    public Hold createHold(Integer userId, Integer timeSlotId, Integer qty, String requestId) throws SQLException {
        // Optimistic check - read current state
        TimeSlot slot = timeSlotDAO.findById(timeSlotId);
        if (slot == null) {
            throw new IllegalArgumentException("Time slot not found");
        }

        if (slot.getCapacityRemaining() < qty) {
            throw new IllegalStateException("Insufficient capacity");
        }

        // Check if user already has a hold on this slot
        Hold existingHold = holdDAO.findByUserAndTimeSlot(userId, timeSlotId);
        if (existingHold != null && !existingHold.isExpired()) {
            throw new IllegalStateException("User already has an active hold on this slot");
        }

        // Create hold (idempotent via requestId)
        if (requestId == null || requestId.isEmpty()) {
            requestId = PasswordUtil.generateRequestId();
        }

        Hold existingByRequestId = holdDAO.findByRequestId(requestId);
        if (existingByRequestId != null) {
            return existingByRequestId;
        }

        Instant expiresAt = Instant.now().plusSeconds(holdDurationSeconds);
        Hold hold = new Hold();
        hold.setUserId(userId);
        hold.setTimeSlotId(timeSlotId);
        hold.setQty(qty);
        hold.setExpiresAt(expiresAt);
        hold.setRequestId(requestId);
        hold.setCreatedAt(Instant.now());

        Integer holdId = holdDAO.create(hold);
        hold.setId(holdId);

        audit("HOLD_CREATED", userId, slot.getResourceId(), 
              String.format("{\"holdId\":%d,\"timeSlotId\":%d,\"qty\":%d}", holdId, timeSlotId, qty));

        publishEvent(new AvailabilityChangedEvent(timeSlotId));
        return hold;
    }

    public Reservation confirmHold(Integer holdId, Integer userId, String requestId) throws SQLException {
        Hold hold = holdDAO.findById(holdId);
        if (hold == null) {
            throw new IllegalArgumentException("Hold not found");
        }
        if (!hold.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Hold does not belong to user");
        }
        if (hold.isExpired()) {
            throw new IllegalStateException("Hold has expired");
        }

        // Critical section: Pessimistic lock
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Lock the time slot row
                TimeSlot slot = timeSlotDAO.findByIdForUpdate(hold.getTimeSlotId());
                if (slot == null) {
                    throw new IllegalArgumentException("Time slot not found");
                }

                // Re-verify capacity
                if (slot.getCapacityRemaining() < hold.getQty()) {
                    conn.rollback();
                    throw new IllegalStateException("Insufficient capacity - slot was taken");
                }

                // Re-verify hold is still valid
                Hold recheckedHold = holdDAO.findById(holdId);
                if (recheckedHold == null || recheckedHold.isExpired()) {
                    conn.rollback();
                    throw new IllegalStateException("Hold is no longer valid");
                }

                // Create reservation (idempotent)
                if (requestId == null || requestId.isEmpty()) {
                    requestId = PasswordUtil.generateRequestId();
                }

                Reservation existing = reservationDAO.findByRequestId(requestId);
                if (existing != null) {
                    conn.rollback();
                    return existing;
                }

                Reservation reservation = new Reservation();
                reservation.setUserId(userId);
                reservation.setTimeSlotId(hold.getTimeSlotId());
                reservation.setQty(hold.getQty());
                reservation.setStatus(Reservation.ReservationStatus.CONFIRMED);
                reservation.setRequestId(requestId);
                reservation.setCreatedAt(Instant.now());

                Integer reservationId = reservationDAO.create(reservation);
                reservation.setId(reservationId);

                // Decrement capacity atomically
                timeSlotDAO.decrementCapacity(hold.getTimeSlotId(), hold.getQty());

                // Delete hold
                holdDAO.delete(holdId);

                conn.commit();

                audit("RESERVATION_CREATED", userId, slot.getResourceId(),
                      String.format("{\"reservationId\":%d,\"timeSlotId\":%d,\"qty\":%d}", 
                                   reservationId, hold.getTimeSlotId(), hold.getQty()));

                logger.info("Reservation {} confirmed for user {}", reservationId, userId);
                publishEvent(new ReservationCreatedEvent(reservationId, hold.getTimeSlotId()));
                publishEvent(new AvailabilityChangedEvent(hold.getTimeSlotId()));
                return reservation;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void cancelHold(Integer holdId, Integer userId) throws SQLException {
        Hold hold = holdDAO.findById(holdId);
        if (hold == null) {
            throw new IllegalArgumentException("Hold not found");
        }
        if (!hold.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Hold does not belong to user");
        }

        holdDAO.delete(holdId);
        TimeSlot slot = timeSlotDAO.findById(hold.getTimeSlotId());
        if (slot != null) {
            audit("HOLD_CANCELLED", userId, slot.getResourceId(),
                  String.format("{\"holdId\":%d,\"timeSlotId\":%d}", holdId, hold.getTimeSlotId()));
            publishEvent(new AvailabilityChangedEvent(hold.getTimeSlotId()));
        }
    }

    public void cancelReservation(Integer reservationId, Integer userId) throws SQLException {
        Reservation reservation = reservationDAO.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found");
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Reservation does not belong to user");
        }
        if (reservation.getStatus() != Reservation.ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Reservation is not confirmed");
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                reservationDAO.cancel(reservationId);
                timeSlotDAO.incrementCapacity(reservation.getTimeSlotId(), reservation.getQty());

                TimeSlot slot = timeSlotDAO.findById(reservation.getTimeSlotId());
                if (slot != null) {
                    audit("RESERVATION_CANCELLED", userId, slot.getResourceId(),
                          String.format("{\"reservationId\":%d,\"timeSlotId\":%d,\"qty\":%d}",
                                       reservationId, reservation.getTimeSlotId(), reservation.getQty()));
                }

                conn.commit();
                logger.info("Reservation {} cancelled by user {}", reservationId, userId);
                publishEvent(new ReservationCancelledEvent(reservationId, reservation.getTimeSlotId()));
                publishEvent(new AvailabilityChangedEvent(reservation.getTimeSlotId()));

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void joinWaitlist(Integer userId, Integer timeSlotId) throws SQLException {
        TimeSlot slot = timeSlotDAO.findById(timeSlotId);
        if (slot == null) {
            throw new IllegalArgumentException("Time slot not found");
        }

        Waitlist existing = waitlistDAO.findByUserAndTimeSlot(userId, timeSlotId);
        if (existing != null) {
            throw new IllegalStateException("User is already on the waitlist");
        }

        Waitlist waitlist = new Waitlist();
        waitlist.setUserId(userId);
        waitlist.setTimeSlotId(timeSlotId);
        waitlist.setQueuedAt(Instant.now());

        waitlistDAO.create(waitlist);

        audit("WAITLIST_JOINED", userId, slot.getResourceId(),
              String.format("{\"timeSlotId\":%d}", timeSlotId));
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

    public List<TimeSlot> getAvailability(Integer resourceId, Instant from, Instant to) throws SQLException {
        return timeSlotDAO.findByResourceAndDateRange(resourceId, from, to);
    }

    // Event classes
    public static class AvailabilityChangedEvent implements Event {
        private final Integer timeSlotId;
        public AvailabilityChangedEvent(Integer timeSlotId) { this.timeSlotId = timeSlotId; }
        @Override public String getType() { return "AVAILABILITY_CHANGED"; }
        public Integer getTimeSlotId() { return timeSlotId; }
    }

    public static class ReservationCreatedEvent implements Event {
        private final Integer reservationId;
        private final Integer timeSlotId;
        public ReservationCreatedEvent(Integer reservationId, Integer timeSlotId) {
            this.reservationId = reservationId;
            this.timeSlotId = timeSlotId;
        }
        @Override public String getType() { return "RESERVATION_CREATED"; }
        public Integer getReservationId() { return reservationId; }
        public Integer getTimeSlotId() { return timeSlotId; }
    }

    public static class ReservationCancelledEvent implements Event {
        private final Integer reservationId;
        private final Integer timeSlotId;
        public ReservationCancelledEvent(Integer reservationId, Integer timeSlotId) {
            this.reservationId = reservationId;
            this.timeSlotId = timeSlotId;
        }
        @Override public String getType() { return "RESERVATION_CANCELLED"; }
        public Integer getReservationId() { return reservationId; }
        public Integer getTimeSlotId() { return timeSlotId; }
    }
}

