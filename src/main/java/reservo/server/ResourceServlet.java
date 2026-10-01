package reservo.server;

import reservo.database.ResourceDAO;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class ResourceServlet extends BaseServlet {
    private final ResourceDAO resourceDAO;

    public ResourceServlet(ResourceDAO resourceDAO) {
        this.resourceDAO = resourceDAO;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        try {
            sendJsonResponse(response, resourceDAO.findAll());
        } catch (SQLException e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}

