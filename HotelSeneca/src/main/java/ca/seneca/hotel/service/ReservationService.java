package ca.seneca.hotel.service;

import ca.seneca.hotel.config.PricingConfig;
import ca.seneca.hotel.events.RoomAvailabilityPublisher;
import ca.seneca.hotel.models.BookingInput;
import ca.seneca.hotel.models.Guest;
import ca.seneca.hotel.models.Invoice;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.models.ReservationStatus;
import ca.seneca.hotel.models.Room;
import ca.seneca.hotel.models.RoomType;
import ca.seneca.hotel.repositories.IReservationRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class ReservationService {

    private final IReservationRepository reservationRepository;
    private final PricingService pricingService;
    private final RoomAvailabilityPublisher roomAvailabilityPublisher;
    private final ActivityLogService activityLogService;

    // Constructor-based dependency injection
    public ReservationService(IReservationRepository reservationRepository,
                              PricingService pricingService,
                              RoomAvailabilityPublisher roomAvailabilityPublisher,
                              ActivityLogService activityLogService) {
        this.reservationRepository = reservationRepository;
        this.pricingService = pricingService;
        this.roomAvailabilityPublisher = roomAvailabilityPublisher;
        this.activityLogService = activityLogService;
    }

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    public Optional<Reservation> getReservationById(Long id) {
        return Optional.ofNullable(reservationRepository.findById(id));
    }

    public Reservation createReservation(Reservation reservation) {
        return reservationRepository.save(reservation);
    }

    public void deleteReservation(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new IllegalArgumentException("Reservation with ID " + id + " does not exist.");
        }
        reservationRepository.deleteById(id);
    }

    /**
     * Turns a completed booking request (kiosk session or admin phone-in booking)
     * into a persisted reservation.
     *
     * @throws IllegalArgumentException if the request is incomplete or invalid
     * @throws IllegalStateException    if not enough rooms are free for those dates
     */
    public Reservation bookFromSession(BookingInput session) {
        validate(session);

        BookingEstimate estimate = pricingService.estimate(session);

        Guest guest = new Guest();
        guest.setFirstName(session.getFirstName().trim());
        guest.setLastName(session.getLastName().trim());
        guest.setPhone(session.getPhone().trim());
        guest.setEmail(session.getEmail().trim());
        guest.setAddress(session.getAddress().trim());
        guest.setCity(session.getCity().trim());
        guest.setPostalCode(session.getPostalCode().trim());
        guest.setLoyaltyMember(session.isEnrolledLoyalty());

        Invoice invoice = new Invoice();
        invoice.setSubtotal(round(estimate.getSubtotal()));
        invoice.setTax(round(estimate.getTax()));
        invoice.setDiscount(round(estimate.getLoyaltyDiscount()));
        invoice.setTotal(round(estimate.getTotal()));
        // Billing is settled at the front desk, so the invoice starts unpaid.
        invoice.setPaid(false);

        Reservation reservation = new Reservation();
        reservation.setCheckInDate(session.getCheckIn());
        reservation.setCheckOutDate(session.getCheckOut());
        reservation.setNumAdults(session.getAdults());
        reservation.setNumChildren(session.getChildren());
        reservation.setStatus(ReservationStatus.BOOKED);
        reservation.setInvoice(invoice);

        Map<RoomType, Integer> roomsNeeded = pricingService.getRequestedRooms(session);

        return reservationRepository.createBooking(
                guest, reservation, roomsNeeded, selectedAddOnNames(session));
    }

    /**
     * Cancels a reservation and lets subscribers (dashboard notifications, the
     * waitlist) know the rooms it held are free again.
     */
    public void cancelReservation(Long id, String actor) {
        Reservation reservation = reservationRepository.findById(id);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation with ID " + id + " does not exist.");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return;
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        for (RoomType type : reservation.getRooms().stream().map(Room::getRoomType).distinct().toArray(RoomType[]::new)) {
            roomAvailabilityPublisher.publish(type, reservation.getCheckInDate(), reservation.getCheckOutDate(),
                    "Reservation #" + id + " cancelled");
        }
        activityLogService.log(actor, "CANCEL", "Reservation", String.valueOf(id), "Reservation cancelled");
    }

    /**
     * Modifies a reservation's dates/room type, reallocating the same number of rooms.
     *
     * @throws IllegalStateException if not enough rooms of the new type are free
     */
    public Reservation modifyReservation(Long id, LocalDate newCheckIn, LocalDate newCheckOut,
                                         RoomType newRoomType, String actor) {
        if (newCheckIn == null || newCheckOut == null || !newCheckOut.isAfter(newCheckIn)) {
            throw new IllegalArgumentException("Check-out date must be after the check-in date.");
        }

        Reservation existing = reservationRepository.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Reservation with ID " + id + " does not exist.");
        }
        RoomType oldType = existing.getRooms().isEmpty() ? null : existing.getRooms().get(0).getRoomType();
        LocalDate oldCheckIn = existing.getCheckInDate();
        LocalDate oldCheckOut = existing.getCheckOutDate();

        Reservation updated = reservationRepository.modifyBooking(id, newCheckIn, newCheckOut, newRoomType);

        if (oldType != null) {
            roomAvailabilityPublisher.publish(oldType, oldCheckIn, oldCheckOut, "Reservation #" + id + " modified");
        }
        activityLogService.log(actor, "MODIFY", "Reservation", String.valueOf(id),
                "Updated to " + newCheckIn + " - " + newCheckOut + ", " + newRoomType);
        return updated;
    }

    /** Rooms of the given type/dates that are free, ignoring the reservation's own current room holds. */
    public long checkAvailability(RoomType type, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        return reservationRepository.countAvailableRooms(type, checkIn, checkOut, excludeReservationId);
    }

    private List<String> selectedAddOnNames(BookingInput session) {
        List<String> names = new ArrayList<>();
        if (session.isWifiSelected())      names.add(PricingConfig.WIFI_NAME);
        if (session.isBreakfastSelected()) names.add(PricingConfig.BREAKFAST_NAME);
        if (session.isParkingSelected())   names.add(PricingConfig.PARKING_NAME);
        if (session.isSpaSelected())       names.add(PricingConfig.SPA_NAME);
        return names;
    }

    private void validate(BookingInput session) {
        requireText(session.getFirstName(), "First name");
        requireText(session.getLastName(), "Last name");
        requireText(session.getPhone(), "Phone");
        requireText(session.getEmail(), "Email");
        requireText(session.getAddress(), "Address");
        requireText(session.getCity(), "City");
        requireText(session.getPostalCode(), "Postal code");

        if (session.getCheckIn() == null || session.getCheckOut() == null) {
            throw new IllegalArgumentException("Check-in and check-out dates are required.");
        }
        if (!session.getCheckOut().isAfter(session.getCheckIn())) {
            throw new IllegalArgumentException("Check-out date must be after the check-in date.");
        }

        Map<RoomType, Integer> rooms = pricingService.getRequestedRooms(session);
        if (rooms.isEmpty()) {
            throw new IllegalArgumentException("At least one room must be selected.");
        }

        int capacity = rooms.entrySet().stream()
                .mapToInt(e -> e.getKey().getMaxOccupancy() * e.getValue())
                .sum();
        int guests = session.getAdults() + session.getChildren();
        if (guests > capacity) {
            throw new IllegalArgumentException(
                    "The selected rooms hold " + capacity + " guest(s) but " + guests + " were entered.");
        }
        if (session.getAdults() < 1) {
            throw new IllegalArgumentException("A booking needs at least one adult.");
        }
    }

    private void requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
