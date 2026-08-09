# Hotel Seneca — Reservation & Front Desk System

A JavaFX desktop application that runs both sides of a small hotel's operations:
a **self-service booking kiosk** for guests, and a **staff administration console**
for front-desk, billing, loyalty, waitlist, and reporting work. Built as a
3-tier, MVC-structured Java application on top of Hibernate/JPA and SQLite.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Design Patterns](#design-patterns)
- [Database](#database)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Project Structure](#project-structure)
- [Development](#development)
- [Known Gotchas](#known-gotchas)
- [Contributors](#contributors)
- [License](#license)

## Overview

Hotel Seneca is a single Maven module (`HotelSeneca/`) that launches into one of
two experiences from a shared welcome screen:

- **Kiosk mode** — a guest walks through a six-step booking flow (guests →
  dates → rooms → add-ons → details → confirmation) with no staff assistance.
- **Staff mode** — an authenticated admin manages reservations, payments,
  checkout, the waitlist, loyalty accounts, and pulls operational reports.

Both flows share the same business and data layers, so pricing, availability,
and loyalty rules never diverge between what a guest sees and what a staff
member sees.

## Features

### Self-service kiosk

- Guest count entry with adult/child validation
- Single-calendar check-in/check-out range picker (tap start date, tap end
  date — no separate pickers)
- Automatic room-plan suggestion based on party size, with the option to pick
  room types/quantities manually
- Live occupancy validation against each room type's capacity
- Add-ons: Wi-Fi, breakfast, parking, spa — each with its own pricing model
  (per-night, per-adult-per-night, or per-reservation)
- Real-time pricing estimate: room subtotal (with weekend-rate detection),
  add-ons, tax, and total
- Loyalty lookup by email, with enrollment for new members
- Guest detail form with inline validation
- Confirmation screen with a generated reservation number
- Rules & Regulations / Room Booking Policy reference panels throughout

### Staff administration

- Staff login (BCrypt-hashed credentials)
- Reservation table: search by guest/phone, filter by date range and status,
  color-coded rows for cancelled/checked-out bookings
- Phone-booking dialog for admin-created reservations, with a **Check
  Availability** lookup (per room type, for the selected dates) and an
  **Add to Waitlist** action when nothing is free
- Reservation edit/cancel, with room-availability notifications published to
  subscribers on cancellation
- Checkout & payment recording (cash/card/loyalty points), including refunds
  with automatic loyalty point reversal/restoration
- Waitlist management with automatic "a room type opened up" matching
- Loyalty dashboard: point balances, enrollment, transaction history
- Guest feedback summary with rating/sentiment filters and CSV export
- Reports: Revenue, Occupancy, Activity Log, Feedback — each filterable by
  date range/room type, exportable to CSV/PDF/TXT
- Full activity audit log for administrative actions

## Tech Stack

| Layer | Technology |
|---|---|
| UI | JavaFX 17 (FXML + CSS), [Ikonli](https://kordamp.org/ikonli/) Material Design icons |
| Dependency injection | Google Guice 7 |
| ORM | Hibernate 5.6 (JPA 2.2) |
| Database | SQLite (via `sqlite-jdbc` + a community SQLite Hibernate dialect) |
| Auth | jBCrypt password hashing |
| Reporting export | Apache PDFBox (PDF), custom CSV/TXT exporters |
| Build | Maven 3.8+, `javafx-maven-plugin` |
| Language level | Java 17 |

## Architecture

The codebase follows a **3-tier architecture** layered under an **MVC**
presentation pattern:

```
┌─────────────────────────────────────────────┐
│  Presentation  — controller/ + resources/view/*.fxml   │
│  JavaFX controllers per screen; FXML defines layout;    │
│  theme.css is the single shared stylesheet.              │
├─────────────────────────────────────────────┤
│  Business      — service/, config/, factory/, events/    │
│  Booking, pricing, loyalty, payment, reporting logic;    │
│  pure Java, no JavaFX or persistence dependencies.        │
├─────────────────────────────────────────────┤
│  Data          — repositories/, models/                  │
│  JPA entities + repository interfaces, backed by         │
│  Hibernate/SQLite implementations.                        │
└─────────────────────────────────────────────┘
```

**Dependency wiring:** `AppContext` is the application's central service
locator — every service/repository singleton is built and cached there.
`AppConfig` (a Guice `AbstractModule`) delegates its `@Provides` bindings back
to `AppContext` rather than constructing objects itself. This is deliberate:
JavaFX's `FXMLLoader` only routes the *first* screen through Guice's
injector — every subsequent `switchScene()` call creates its controller
directly, bypassing the injector. Routing both paths through `AppContext`
guarantees the whole app shares one instance of each service regardless of
which path constructed the controller.

**Entry point:** `AppLauncher` (a plain class, not a JavaFX `Application`) →
`MainApp` (bootstraps Guice, opens `WelcomeView.fxml`) → `AppConfig`. This
indirection exists to avoid JavaFX's module-path runtime checks on the main
class.

## Design Patterns

| Pattern | Where | Why |
|---|---|---|
| **Factory** | `factory/RoomFactory` | Builds `Room` prototypes by `RoomType` so pricing/availability code never hardcodes room construction. |
| **Strategy** | `service/PricingStrategy` + `StandardPricingStrategy` / `WeekendPricingStrategy`; `service/billing/BillingStrategy` + `StandardBillingStrategy`, `PercentageDiscountStrategy`, `LoyaltyRedemptionStrategy` | Swaps nightly-rate calculation and checkout discount/redemption logic without touching call sites. |
| **Decorator** | `service/pricing/Billable`, `RoomCharge`, `AddOnDecorator` + `WifiDecorator`, `BreakfastDecorator`, `ParkingDecorator`, `SpaDecorator` | Each selected add-on wraps the running bill with its own cost, in any combination, without a combinatorial explosion of pricing methods. |
| **Observer** | `events/RoomAvailabilityPublisher` / `RoomAvailabilityObserver`, with `WaitlistService` and `NotificationCenter` as subscribers | Cancelling/checking out a reservation frees rooms; observers react (flag matching waitlist entries, push dashboard notifications) without the reservation flow knowing they exist. |
| **Singleton** | `config/AppContext` (service locator), `models/KioskSession` (per-run guest booking state), `util/JpaUtil`'s shared `EntityManagerFactory` | One source of truth for app-wide services, in-progress kiosk state, and the JPA connection pool. |
| **Repository** | `repositories/I*Repository` interfaces + `Jpa*Repository` implementations | Isolates JPA/Hibernate specifics behind plain interfaces the service layer depends on. |
| **Dependency Injection** | Constructor injection throughout `service/`, wired via Guice (`AppConfig`) | Testable, decoupled services — nothing reaches for a global except through its constructor. |

## Database

SQLite, managed by Hibernate with `hibernate.hbm2ddl.auto=update` (schema is
created/updated automatically at startup; no manual migrations needed for
local development).

- Full entity-relationship diagram: [`docs/ERD diagram.png`](docs/ERD%20diagram.png)
- Table-by-table purpose reference: [`docs/ERD_TABLE_OVERVIEW.md`](docs/ERD_TABLE_OVERVIEW.md)
- Booking flow sequence diagram: [`docs/BookingFlowSequenceDiagram.png`](docs/BookingFlowSequenceDiagram.png)

Core entities: `Guest`, `Room` (+ `RoomType`), `Reservation` (+
`ReservationStatus`), `Invoice`, `AddOn`, `Payment` (+ `PaymentMethod`),
`LoyaltyTransaction` (+ `LoyaltyTxnType`), `WaitlistEntry` (+
`WaitlistStatus`), `Feedback` (+ `SentimentTag`), `AdminUser` (+ `Role`),
`ActivityLog`.

The room inventory and add-on catalogue are seeded automatically on first run
(`service/DataSeeder`) if the tables are empty.

## Getting Started

### Prerequisites

- JDK 17
- Maven 3.8+
- (JavaFX runtime dependencies are pulled automatically by Maven — no local
  JavaFX SDK install needed.)

```bash
java -version
mvn -version
```

### Run

```bash
cd HotelSeneca
mvn clean javafx:run
```

The app opens at `1000×700` (resizable/maximizable) on the welcome screen,
with **Start Booking** (kiosk) and **Staff Login** entry points.

A default admin account is seeded on first run — check `service/DataSeeder`
for the seeded credentials, or create an admin user directly if you're
starting from a fresh database.

## Configuration

All tunable settings live in code rather than external config files:

- **Pricing** — `config/PricingConfig` (add-on prices/charging model, tax
  rate, weekend multiplier), `config/DiscountPolicy`, `config/LoyaltyPolicy`
  (point-earn rate, redemption rate/cap). Room base prices live on the
  `RoomType` enum itself.
- **Persistence** — `src/main/resources/META-INF/persistence.xml` (JDBC URL,
  Hibernate dialect, schema-generation strategy).
- **Logging** — `util/LoggerService` writes a rotating log
  (`system_logs.N.log`, 1&nbsp;MB × 10 files) plus console output.

> ⚠️ A few prices/labels are intentionally duplicated between `PricingConfig`
> / `RoomType` and their corresponding kiosk FXML display text (documented
> in-code where this applies). If you change a price, update both.

## Project Structure

```text
HotelSeneca/
├── pom.xml
├── database.db                      # SQLite file (gitignored; created at first run)
├── src/main/java/
│   ├── AppLauncher.java             # Entry point (bypasses JavaFX module-path checks)
│   └── ca/seneca/hotel/
│       ├── app/                     # MainApp (bootstrap), AppConfig (Guice module)
│       ├── config/                  # AppContext (service locator), pricing/loyalty/discount config
│       ├── controller/
│       │   ├── kiosk/               # One controller per kiosk screen + DateRangeCalendar (custom control)
│       │   └── admin/               # Dashboard, booking, checkout, payment, loyalty, waitlist
│       │       └── reports/         # Revenue, Occupancy, Activity Log, Feedback report controllers
│       ├── events/                  # Observer pattern: RoomAvailabilityPublisher/Observer, NotificationCenter
│       ├── factory/                 # RoomFactory
│       ├── models/                  # JPA entities + enums
│       ├── navigation/              # Scene-switching helpers
│       ├── repositories/            # I*Repository interfaces + Jpa*Repository implementations
│       ├── security/                # AuthService, BCryptPasswordHasher, CurrentSession
│       ├── service/                 # Business logic (booking, payment, loyalty, reporting, ...)
│       │   ├── billing/             # Strategy pattern: discount/redemption/billing strategies
│       │   └── pricing/             # Decorator pattern: add-on cost decorators
│       └── util/                    # JPA transaction helper, log/CSV/PDF/TXT export utilities
├── src/main/resources/
│   ├── META-INF/persistence.xml     # JPA/Hibernate configuration
│   └── view/
│       ├── theme.css                # Single shared stylesheet for every screen
│       ├── kiosk/                   # Guest-facing booking flow FXML
│       └── admin/                   # Staff-facing FXML (+ admin/reports/)
docs/
├── ERD diagram.png
├── ERD_TABLE_OVERVIEW.md
└── BookingFlowSequenceDiagram.png
```

## Development

Compile without launching the UI:

```bash
mvn clean compile
```

Run with Maven diagnostics if startup fails:

```bash
mvn clean javafx:run -e
```

Full Maven debug output:

```bash
mvn javafx:run -X
```

The first `Caused by:` section in the Maven output is almost always the
actual underlying error — Hibernate/JavaFX wrap it in several layers of
`InvocationTargetException`/`LoadException` on top.

There is currently no automated test suite (`src/test` is empty) — verify
changes by running the app and exercising the relevant kiosk/admin flow.

## Known Gotchas

- **SQLite timestamp columns**: `persistence.xml` explicitly sets
  `date_class=TEXT&date_string_format=yyyy-MM-dd HH:mm:ss.SSS` on the JDBC
  URL. Without it, `sqlite-jdbc` writes `Timestamp` columns as a raw
  epoch-millis number that its own `getTimestamp()` then fails to parse back
  — do not remove these query parameters.
- **`hbm2ddl.auto=update` on SQLite** only detects *new* columns/tables
  reliably; it will not always widen/alter an existing column. If you add a
  `@Column` to an existing entity and the app throws `no such column: ...`,
  you may need to add it to `database.db` manually (or delete the local
  `database.db` to let it be reseeded from scratch, if you don't need to keep
  local data).
- FXML files list their `<?import ...?>`s individually in most kiosk views
  but use wildcard imports (`javafx.scene.text.*`, etc.) in most admin
  views — when adding a new control type to a kiosk FXML, double-check the
  import is actually present.

## Contributors

- Suganya Maheswaran
- Miiyaco
- sbikcs

## License

No license has been applied to this repository. This project was developed
as an academic assignment; treat it as all-rights-reserved unless the
authors state otherwise.
