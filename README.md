# Reservo: A Transaction-Safe Multi-Resource Reservation System

**CS9053 Final Project - Fall 2025**  
**Team**: Snigdha Srivastva (ss19776), Utkarsh Mittal (um2100)

## Overview

Reservo is a campus resource reservation system designed to handle concurrent bookings safely and fairly. It supports study rooms, sports courts, lab benches, and clinic slots with real-time availability updates, hold/confirm workflows, waitlist promotion, and comprehensive audit trails.

## Key Features

- **Transaction-Safe Concurrent Booking**: Pessimistic locking ensures single-winner guarantees for contested slots
- **Real-Time Updates**: WebSocket push notifications update all clients instantly
- **Hold/Confirm Workflow**: Short-lived holds with countdown timers prevent stale bookings
- **Automatic Waitlist Promotion**: FIFO queue automatically promotes users when capacity frees
- **Audit Trail**: Complete append-only log of all state transitions
- **Admin Console**: Resource management and isolation level demonstration tools

## Tech Stack

- **Java 21**
- **JavaFX** for desktop client UI
- **Jetty** for HTTP server and WebSocket support
- **JDBC** with H2 (dev) / PostgreSQL (production)
- **Maven** for build management
- **JUnit 5** + **Testcontainers** for testing

## Project Structure

```
reservo/
├── src/main/java/reservo/
│   ├── client/          # JavaFX client application
│   ├── server/          # HTTP/WebSocket server
│   ├── database/        # JDBC data access layer
│   ├── model/           # Domain models
│   ├── service/         # Business logic
│   └── common/          # Shared utilities
├── src/main/resources/
│   ├── migrations/      # SQL migration scripts
│   └── seed.sql         # Initial test data
├── src/test/java/       # Integration and unit tests
└── pom.xml
```

## Building and Running

### Prerequisites

- Java 21 or higher
- Maven 3.8+
- PostgreSQL (for production) or H2 (included, for development)

### Build

```bash
mvn clean compile
```

### Run Server

```bash
mvn exec:java -Dexec.mainClass="reservo.server.ReservoServer"
```

Or build and run the server JAR:

```bash
mvn clean package
java -jar target/reservo-1.0.0-jar-with-dependencies.jar server
```

### Run Client

```bash
mvn javafx:run
```

Or build and run separately:

```bash
mvn clean package
java --module-path <javafx-path> --add-modules javafx.controls,javafx.fxml -jar target/reservo-client.jar
```

## Database Setup

### Development (H2 - in-memory)

The server automatically creates an H2 database on startup when using the default configuration.

**Note**: The seed data includes test users, but you'll need to register new users via the API or update the password hashes. Default test credentials:
- Username: `admin`, Password: `admin123` (if hash is correct)
- Username: `alice`, Password: `test123` (if hash is correct)

For production use, register users via `POST /auth/register` endpoint.

### Production (PostgreSQL)

1. Create a PostgreSQL database:
```sql
CREATE DATABASE reservo;
```

2. Run migrations:
```bash
# Migrations are automatically applied on server startup
# Or run manually: psql -d reservo -f src/main/resources/migrations/001_initial_schema.sql
```

3. Update `config.properties`:
```properties
db.url=jdbc:postgresql://localhost:5432/reservo
db.user=your_user
db.password=your_password
```

## API Endpoints

### Authentication
- `POST /auth/register` - Register new user
- `POST /auth/login` - Login and get session token

### Discovery
- `GET /resources` - List all resources
- `GET /availability?resourceId={id}&from={iso-date}&to={iso-date}` - Get availability

### Booking
- `POST /holds` - Create a hold on a time slot
- `POST /holds/{id}/confirm` - Confirm a hold (creates reservation)
- `DELETE /holds/{id}` - Cancel a hold
- `POST /reservations/{id}/cancel` - Cancel a reservation
- `POST /waitlist` - Join waitlist for a slot

### Events
- `WebSocket /ws` - Real-time event stream

### Admin
- `POST /resources` - Create/edit resource
- `POST /admin/simulate-contention` - Simulate concurrent booking attempts
- `POST /admin/isolation-mode` - Change database isolation level

## Demo Scenarios

1. **Contention Test**: Two clients race for the last seat; observe single-winner guarantee
2. **Waitlist Promotion**: Cancel a reservation and see automatic promotion of next in queue
3. **Isolation Demo**: Toggle between READ COMMITTED and SERIALIZABLE to see phantom read prevention
4. **Real-Time Updates**: Open multiple clients and see instant updates across all windows

## Testing

Run all tests:

```bash
mvn test
```

Run integration tests (requires Docker for Testcontainers):

```bash
mvn verify
```

## License

This is an academic project for CS9053 at NYU.

