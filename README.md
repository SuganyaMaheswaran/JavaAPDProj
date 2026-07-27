# Hotel Seneca Reservation System

A JavaFX desktop application for hotel reservations, kiosk booking, and staff administration.

## Requirements

- Java Development Kit (JDK) 17
- Maven 3.8 or newer
- JavaFX 17.0.10 dependencies are downloaded by Maven

Verify the installed versions:

```bash
java -version
mvn -version
```

## Run the application

From the project directory:

```bash
cd HotelSeneca
mvn clean javafx:run
```

The application opens at `1000 x 700` and can be resized or maximized.

## Main features

### Self-service kiosk

- Guest and child count selection
- Check-in and check-out date validation
- Room suggestions based on occupancy
- Manual room quantity selection
- Room availability checks
- Add-ons for Wi-Fi, breakfast, parking, and spa
- Pricing estimate with subtotal, tax, discounts, and total
- Guest detail validation
- Reservation confirmation with a confirmation number
- Rules & Regulations and Room Booking Policy alert boxes throughout the booking flow

### Staff administration

- Staff login screen
- Admin dashboard navigation
- Reservation table populated from persisted reservations
- Reservation search by guest name, phone, date range, and status
- Payment, waitlist, loyalty, and reporting screens

## Architecture

The project follows a three-tier structure:

- **Presentation:** JavaFX controllers and FXML views
- **Business:** Services for booking, pricing, validation, and application rules
- **Data:** Repository interfaces and JPA-backed SQLite persistence

Important packages:

```text
src/main/java/
├── app/             Application bootstrap and dependency injection
├── config/          Shared application dependencies and pricing configuration
├── controller/      JavaFX controllers
├── models/          JPA entities and domain models
├── repositories/    Repository interfaces and JPA implementations
├── service/         Booking, pricing, and database seeding services
├── navigation/      Scene navigation helpers
└── util/            JPA transaction utilities
```

FXML views are stored under:

```text
src/main/resources/view/
```

## Persistence

The application uses:

- JPA/Hibernate for ORM
- SQLite for local persistence
- A singleton `EntityManagerFactory`
- A separate `EntityManager` for each transaction

The SQLite database is created as:

```text
database.db
```

The room inventory and add-on catalogue are seeded during application startup.

## Development notes

Compile without starting JavaFX:

```bash
mvn clean compile
```

Run with detailed Maven diagnostics if startup fails:

```bash
mvn clean javafx:run -e
```

For full Maven debug output:

```bash
mvn javafx:run -X
```

The first `Caused by:` section in the Maven output usually contains the underlying startup error.
