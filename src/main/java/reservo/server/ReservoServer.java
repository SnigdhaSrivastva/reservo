package reservo.server;

import reservo.database.*;
import reservo.service.*;
import reservo.common.Config;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;

/**
 * Main server class that starts the HTTP server and WebSocket endpoint.
 */
public class ReservoServer {
    private static final Logger logger = LoggerFactory.getLogger(ReservoServer.class);
    private Server server;
    private SchedulerService scheduler;

    public static void main(String[] args) {
        ReservoServer reservoServer = new ReservoServer();
        reservoServer.start();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down server...");
            reservoServer.stop();
        }));
    }

    public void start() {
        try {
            logger.info("Initializing Reservo server...");
            
            // Initialize database
            DatabaseManager.initialize();
            MigrationRunner.runMigrations();
            
            // Set isolation level
            String isolationLevel = Config.get("db.isolation.level", "READ_COMMITTED");
            DatabaseManager.setIsolationLevel(isolationLevel);
            
            // Initialize DAOs
            UserDAO userDAO = new UserDAO();
            ResourceDAO resourceDAO = new ResourceDAO();
            TimeSlotDAO timeSlotDAO = new TimeSlotDAO();
            HoldDAO holdDAO = new HoldDAO();
            ReservationDAO reservationDAO = new ReservationDAO();
            WaitlistDAO waitlistDAO = new WaitlistDAO();
            NotificationDAO notificationDAO = new NotificationDAO();
            AuditDAO auditDAO = new AuditDAO();
            
            // Initialize services
            AuthService authService = new AuthService(userDAO);
            int holdDuration = Config.getInt("hold.duration_seconds", 300);
            BookingService bookingService = new BookingService(timeSlotDAO, holdDAO, reservationDAO,
                                                              waitlistDAO, notificationDAO, auditDAO,
                                                              resourceDAO, holdDuration);
            
            // Initialize scheduler
            int cleanupInterval = Config.getInt("hold.cleanup_interval_seconds", 30);
            int promotionInterval = Config.getInt("waitlist.promotion_interval_seconds", 10);
            scheduler = new SchedulerService(holdDAO, waitlistDAO, timeSlotDAO, reservationDAO,
                                            notificationDAO, auditDAO, bookingService,
                                            cleanupInterval, promotionInterval);
            
            // Start scheduler
            scheduler.start();
            
            // Setup WebSocket event publisher
            WebSocketManager wsManager = new WebSocketManager();
            WebSocketEndpoint.setWebSocketManager(wsManager);
            scheduler.setEventPublisher(event -> wsManager.broadcast(event));
            bookingService.setEventPublisher(event -> wsManager.broadcast(event));
            
            // Create server
            int port = Config.getInt("server.port", 8080);
            server = new Server(port);
            
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            server.setHandler(context);
            
            // Configure WebSocket
            JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
                wsContainer.addEndpoint(WebSocketEndpoint.class);
            });
            
            // Register REST API servlets
            context.addServlet(new ServletHolder(new AuthServlet(authService)), "/auth/*");
            context.addServlet(new ServletHolder(new ResourceServlet(resourceDAO)), "/resources");
            context.addServlet(new ServletHolder(new AvailabilityServlet(timeSlotDAO)), "/availability");
            context.addServlet(new ServletHolder(new BookingServlet(bookingService, authService, wsManager)), "/holds/*");
            context.addServlet(new ServletHolder(new BookingServlet(bookingService, authService, wsManager)), "/reservations/*");
            context.addServlet(new ServletHolder(new BookingServlet(bookingService, authService, wsManager)), "/waitlist");
            context.addServlet(new ServletHolder(new AdminServlet(resourceDAO, bookingService, authService)), "/admin/*");
            context.addServlet(new ServletHolder(new NotificationServlet(notificationDAO, authService)), "/notifications");
            
            // Start server
            server.start();
            logger.info("Reservo server started on port {}", port);
            logger.info("API available at http://localhost:{}/", port);
            logger.info("WebSocket available at ws://localhost:{}/ws", port);
            
            server.join();
            
        } catch (Exception e) {
            logger.error("Failed to start server", e);
            System.exit(1);
        }
    }

    public void stop() {
        try {
            if (scheduler != null) {
                scheduler.stop();
            }
            if (server != null && server.isStarted()) {
                server.stop();
            }
            DatabaseManager.close();
        } catch (Exception e) {
            logger.error("Error stopping server", e);
        }
    }
}

