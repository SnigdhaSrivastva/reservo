package reservo.server;

import reservo.database.TimeSlotDAO;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class AvailabilityServlet extends BaseServlet {
    private final TimeSlotDAO timeSlotDAO;

    public AvailabilityServlet(TimeSlotDAO timeSlotDAO) {
        this.timeSlotDAO = timeSlotDAO;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        try {
            String resourceIdStr = request.getParameter("resourceId");
            String fromStr = request.getParameter("from");
            String toStr = request.getParameter("to");

            if (resourceIdStr == null || fromStr == null || toStr == null) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing required parameters");
                return;
            }

            Integer resourceId = Integer.parseInt(resourceIdStr);
            Instant from = Instant.parse(fromStr);
            Instant to = Instant.parse(toStr);

            sendJsonResponse(response, timeSlotDAO.findByResourceAndDateRange(resourceId, from, to));
        } catch (NumberFormatException | java.time.format.DateTimeParseException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter format");
        } catch (SQLException e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}

