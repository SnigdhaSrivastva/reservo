package reservo.server;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Set;

/**
 * Manages WebSocket connections and broadcasts events to all connected clients.
 */
public class WebSocketManager {
    private final Set<WebSocketEndpoint> connections = new CopyOnWriteArraySet<>();

    public void addConnection(WebSocketEndpoint endpoint) {
        connections.add(endpoint);
    }

    public void removeConnection(WebSocketEndpoint endpoint) {
        connections.remove(endpoint);
    }

    public void broadcast(Object event) {
        String message = serializeEvent(event);
        connections.forEach(endpoint -> {
            try {
                endpoint.sendMessage(message);
            } catch (Exception e) {
                // Connection may be closed, remove it
                removeConnection(endpoint);
            }
        });
    }

    private String serializeEvent(Object event) {
        // Simple JSON serialization - in production use Jackson
        if (event instanceof SchedulerService.AvailabilityChangedEvent) {
            SchedulerService.AvailabilityChangedEvent e = (SchedulerService.AvailabilityChangedEvent) event;
            return String.format("{\"type\":\"AVAILABILITY_CHANGED\",\"timeSlotId\":%d}", e.getTimeSlotId());
        } else if (event instanceof SchedulerService.PromotedEvent) {
            SchedulerService.PromotedEvent e = (SchedulerService.PromotedEvent) event;
            return String.format("{\"type\":\"PROMOTED\",\"userId\":%d,\"timeSlotId\":%d,\"reservationId\":%d}",
                               e.getUserId(), e.getTimeSlotId(), e.getReservationId());
        } else if (event instanceof BookingService.AvailabilityChangedEvent) {
            BookingService.AvailabilityChangedEvent e = (BookingService.AvailabilityChangedEvent) event;
            return String.format("{\"type\":\"AVAILABILITY_CHANGED\",\"timeSlotId\":%d}", e.getTimeSlotId());
        } else if (event instanceof BookingService.ReservationCreatedEvent) {
            BookingService.ReservationCreatedEvent e = (BookingService.ReservationCreatedEvent) event;
            return String.format("{\"type\":\"RESERVATION_CREATED\",\"reservationId\":%d,\"timeSlotId\":%d}",
                               e.getReservationId(), e.getTimeSlotId());
        } else if (event instanceof BookingService.ReservationCancelledEvent) {
            BookingService.ReservationCancelledEvent e = (BookingService.ReservationCancelledEvent) event;
            return String.format("{\"type\":\"RESERVATION_CANCELLED\",\"reservationId\":%d,\"timeSlotId\":%d}",
                               e.getReservationId(), e.getTimeSlotId());
        }
        return "{\"type\":\"UNKNOWN\"}";
    }
}

