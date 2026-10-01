package reservo.server;

import reservo.service.AuthService;
import reservo.service.BookingService;
import com.fasterxml.jackson.databind.JsonNode;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class BookingServlet extends BaseServlet {
    private final BookingService bookingService;
    private final AuthService authService;
    private final WebSocketManager wsManager;

    public BookingServlet(BookingService bookingService, AuthService authService, WebSocketManager wsManager) {
        this.bookingService = bookingService;
        this.authService = authService;
        this.wsManager = wsManager;
    }

    /** Servlet path + path info, e.g. "/holds", "/holds/7/confirm", "/reservations/3/cancel". */
    private static String fullPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return request.getServletPath() + (pathInfo == null ? "" : pathInfo);
    }

    private Integer getUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            AuthService.UserSession session = authService.validateSession(token);
            if (session != null) {
                return session.getUser().getId();
            }
        }
        return null;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String path = fullPath(request);

        Integer userId = getUserId(request);
        if (userId == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated");
            return;
        }

        try {
            JsonNode json = objectMapper.readTree(request.getInputStream());

            if (path.startsWith("/holds") && !path.contains("/confirm") && !path.contains("/cancel")) {
                // Create hold
                Integer timeSlotId = json.get("timeSlotId").asInt();
                Integer qty = json.has("qty") ? json.get("qty").asInt() : 1;
                String requestId = json.has("requestId") ? json.get("requestId").asText() : null;
                
                var hold = bookingService.createHold(userId, timeSlotId, qty, requestId);
                sendJsonResponse(response, hold);
                
            } else if (path.contains("/confirm")) {
                // Confirm hold
                String[] parts = path.split("/");
                Integer holdId = Integer.parseInt(parts[parts.length - 2]);
                String requestId = json.has("requestId") ? json.get("requestId").asText() : null;
                
                var reservation = bookingService.confirmHold(holdId, userId, requestId);
                sendJsonResponse(response, reservation);
                
            } else if (path.startsWith("/waitlist")) {
                // Join waitlist
                Integer timeSlotId = json.get("timeSlotId").asInt();
                bookingService.joinWaitlist(userId, timeSlotId);
                sendJsonResponse(response, new ResultMessage("Successfully joined waitlist"));
                
            } else if (path.contains("/cancel") && path.contains("/reservations")) {
                // Cancel reservation
                String[] parts = path.split("/");
                Integer reservationId = Integer.parseInt(parts[parts.length - 2]);
                bookingService.cancelReservation(reservationId, userId);
                sendJsonResponse(response, new ResultMessage("Reservation cancelled"));
                
            } else {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String path = fullPath(request);

        Integer userId = getUserId(request);
        if (userId == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated");
            return;
        }

        try {
            if (path.startsWith("/holds/")) {
                String[] parts = path.split("/");
                Integer holdId = Integer.parseInt(parts[parts.length - 1]);
                bookingService.cancelHold(holdId, userId);
                sendJsonResponse(response, new ResultMessage("Hold cancelled"));
            } else {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private static class ResultMessage {
        public String message;
        public ResultMessage(String message) { this.message = message; }
    }
}

