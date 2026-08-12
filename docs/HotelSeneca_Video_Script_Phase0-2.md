# Hotel Seneca — Video Submission Script (Phases 0–2)
**Runtime target:** ~5 minutes, solo narration
**Speaker:** Suganya Maheswaran

---

## [0:00–0:30] Intro

"Hi, I'm Suganya, and this is Hotel Seneca — a JavaFX desktop app for running a hotel's front desk: reservations, billing, loyalty, and reporting. It's built as a 3-tier, MVC-structured Java app on top of Hibernate and SQLite, wired together with Guice for dependency injection.

For this submission, I'll walk through the first three phases of the build: the architectural foundation, real staff authentication, and reservation CRUD."

*(Show: WelcomeView.fxml — Start Booking / Staff Login screen)*

---

## [0:30–2:00] Phase 0 — Foundation

"Phase 0 was about laying down the skeleton everything else builds on, before any screen was fully wired up.

**Data layer:** I defined the core JPA entities — `Guest`, `Room`, `Reservation`, `Invoice`, `Payment`, `AdminUser`, `WaitlistEntry`, `Feedback`, `ActivityLog` — and put a repository interface in front of each one, so the service layer never talks to Hibernate directly. That's the Repository pattern: `IReservationRepository` is just a contract, and `JpaReservationRepository` is the only class that knows SQLite or Hibernate exist.

**Security:** `AuthService` and `BCryptPasswordHasher` — passwords are never stored in plain text, and every login attempt, success or failure, gets written to the activity log through `ActivityLogService`.

**Events:** I set up the Observer pattern early — `RoomAvailabilityPublisher` and `RoomAvailabilityObserver` — so that later, when a reservation is cancelled, anything that cares about a freed-up room — like the waitlist — can react without the reservation code knowing they exist.

**Billing:** the Decorator pattern went in here too — `Billable`, `RoomCharge`, and add-on decorators like `WifiDecorator` and `BreakfastDecorator` — so any combination of add-ons can wrap the running bill without a combinatorial explosion of pricing methods.

**Wiring:** all of this is tied together through `AppContext`, a service locator that Guice's `AppConfig` module delegates to — that's what guarantees the kiosk and admin sides of the app always share the same service instances.

And to make Phase 1 testable, `DataSeeder` seeds a default admin account on first run."

*(Show: brief look at project structure — models/, repositories/, service/, security/ packages)*

---

## [2:00–3:00] Phase 1 — Real Authentication

"Phase 1 made login actually work end-to-end instead of being stubbed out.

`LoginViewController` collects the username and password and hands them to `AuthService.login()`, which looks up the `AdminUser` by username and verifies the password against the stored BCrypt hash. Every attempt — success or failure — gets logged.

On success, `CurrentSession` holds onto who's logged in for the rest of the app's lifetime, and the admin dashboard reads from it to show the logged-in staff member's name — so it's not just 'you're in,' the whole admin session actually knows who you are."

*(Show: Staff Login screen → enter seeded admin credentials → land on AdminDashboard, point at the logged-in user's name displayed)*

---

## [3:00–4:30] Phase 2 — Reservation CRUD

"Phase 2 is where staff actually manage bookings.

From the dashboard, admins can create a phone booking through `AdminNewReservationController`. As you pick check-in and check-out dates, it calls `ReservationService.checkAvailability()` live, per room type, so staff see exactly how many rooms are free for those dates before committing."

*(Show: open AdminNewReservationDialog, pick dates, show the availability table updating per room type)*

"If a room type is fully booked for those dates, the dialog can send the guest to the waitlist instead of just failing.

Editing works the same way — staff can change guest counts, room quantities, or add-ons on an existing reservation, but guest contact info stays locked to prevent identity mix-ups. And cancelling a reservation doesn't just delete a row — it publishes an availability event through the Observer pattern I mentioned in Phase 0, so the waitlist can immediately pick up the freed room."

*(Show: edit an existing reservation, then cancel one, and — if time allows — show the activity log or waitlist reacting)*

---

## [4:30–5:00] Wrap-up

"So that's Phase 0 through 2: a repository-backed data layer with the Factory, Strategy, Decorator, and Observer patterns already in place, real BCrypt-based staff authentication with session tracking, and full reservation CRUD with live availability checks and conflict-aware cancellation.

Everything from here — checkout, payments, loyalty, waitlisting, and reporting — builds directly on this foundation. Thanks for watching."

*(Show: return to AdminDashboard, fade out)*

---

### Notes for recording
- Total spoken word count is ~620 words, which lands close to 5 minutes at a natural pace (~125–130 wpm) — trim the Phase 0 section first if you're running long, since it has the least on-screen demo to anchor it.
- Swap in your actual seeded admin credentials from `DataSeeder` before recording the login demo.
- If you want to show code instead of just narrating it, the four files worth having open are: `AppContext.java`, `AuthService.java`, `RoomAvailabilityPublisher.java`, and `AdminNewReservationController.java`.
