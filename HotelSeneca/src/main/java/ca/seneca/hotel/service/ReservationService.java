package ca.seneca.hotel.service;

import ca.seneca.hotel.config.PricingConfig;
import ca.seneca.hotel.events.RoomAvailabilityPublisher;
import ca.seneca.hotel.models.AddOn;
import ca.seneca.hotel.models.AdminBookingRequest;
import ca.seneca.hotel.models.BookingInput;
import ca.seneca.hotel.models.Guest;
import ca.seneca.hotel.models.Invoice;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.models.ReservationStatus;
import ca.seneca.hotel.models.Room;
import ca.seneca.hotel.models.RoomType;
import ca.seneca.hotel.repositories.IPaymentRepository;
import ca.seneca.hotel.repositories.IReservationRepository;
import ca.seneca.hotel.security.CurrentSession;

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
    private final LoyaltyService loyaltyService;
    private final IPaymentRepository paymentRepository;

    // Constructor-based dependency injection
    public ReservationService(IReservationRepository reservationRepository,
                              PricingService pricingService,
                              RoomAvailabilityPublisher roomAvailabilityPublisher,
                              ActivityLogService activityLogService,
                              LoyaltyService loyaltyService,
                              IPaymentRepository paymentRepository) {
        this.reservationRepository = reservationRepository;
        this.pricingService = pricingService;
        this.roomAvailabilityPublisher = roomAvailabilityPublisher;
        this.activityLogService = activityLogService;
        this.loyaltyService = loyaltyService;
        this.paymentRepository = paymentRepository;
    }

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    public Optional<Reservation> getReservationById(Long id) {
        return Optional.ofNullable(reservationRepository.findById(id));
    }

    /** Finds a guest's reservations using either the booking email or a 10-digit phone number. */
    public List<Reservation> findReservationsByGuestContact(String contact) {
        String value = contact == null ? "" : contact.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Please enter your phone number or email address.");
        }

        String email = "";
        String phoneDigits = "";
        if (value.contains("@")) {
            if (!value.matches("^[^@\\s]+@[^@\\s.]+\\.[^@\\s]+$")) {
                throw new IllegalArgumentException("Please enter a valid email address.");
            }
            email = value.toLowerCase();
        } else {
            phoneDigits = value.replaceAll("\\D", "");
            if (phoneDigits.length() != 10) {
                throw new IllegalArgumentException("Please enter a complete 10-digit phone number.");
            }
        }
        return reservationRepository.findByGuestContact(email, phoneDigits);
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
        // Membership is granted by LoyaltyService after the guest is persisted, because issuing a loyalty number needs the generated id.
        guest.setLoyaltyMember(false);

        Invoice invoice = new Invoice();
        invoice.setSubtotal(round(estimate.getSubtotal()));
        invoice.setTax(round(estimate.getTax()));
        // Admin discounts and loyalty point redemption are applied later, at checkout.
        invoice.setDiscount(0.0);
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

        Reservation saved = reservationRepository.createBooking(
                guest, reservation, roomsNeeded, selectedAddOnNames(session));

        if (session.isEnrollRequested()) {
            // Enrol now: the guest row exists, so a loyalty number can be issued.
            loyaltyService.enroll(saved.getGuest(), CurrentSession.actorName());
        }
        return saved;
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
     * Checks a booked guest in on arrival. Only a BOOKED reservation can be checked
     * in -- this is what unlocks checkout, so a reservation can't skip straight from
     * booked to checked out.
     */
    public void checkInReservation(Long id, String actor) {
        Reservation reservation = reservationRepository.findById(id);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation with ID " + id + " does not exist.");
        }
        if (reservation.getStatus() != ReservationStatus.BOOKED) {
            throw new IllegalStateException(
                    "Only a booked reservation can be checked in (current status: " + reservation.getStatus() + ").");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.save(reservation);
        activityLogService.log(actor, "CHECK_IN", "Reservation", String.valueOf(id), "Guest checked in");
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
        if (newRoomType == null) {
            throw new IllegalArgumentException("Room type is required.");
        }

        Reservation existing = reservationRepository.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Reservation with ID " + id + " does not exist.");
        }
        if (existing.getStatus() == ReservationStatus.CANCELLED
                || existing.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new IllegalStateException("A " + existing.getStatus() + " reservation cannot be modified.");
        }

        RoomType oldType = existing.getRooms().isEmpty() ? null : existing.getRooms().get(0).getRoomType();
        LocalDate oldCheckIn = existing.getCheckInDate();
        LocalDate oldCheckOut = existing.getCheckOutDate();
        double oldTotal = existing.getInvoice().getTotal();

        int roomCount = Math.max(1, existing.getRooms().size());
        BookingEstimate estimate = pricingService.estimate(
                pricingInputForEdit(existing, newCheckIn, newCheckOut, newRoomType, roomCount));

        double newSubtotal = round(estimate.getSubtotal());
        double newTax = round(estimate.getTax());
        double newGross = round(newSubtotal + newTax);
        double oldGross = existing.getInvoice().getSubtotal() + existing.getInvoice().getTax();
        double discountRate = oldGross <= 0 ? 0
                : Math.max(0, Math.min(1, existing.getInvoice().getDiscount() / oldGross));
        double newDiscount = round(newGross * discountRate);
        double newTotal = round(newGross - newDiscount);
        double totalPaid = paymentRepository.findByReservationId(id).stream()
                .mapToDouble(payment -> payment.getAmount())
                .sum();

        Invoice repricedInvoice = new Invoice();
        repricedInvoice.setSubtotal(newSubtotal);
        repricedInvoice.setTax(newTax);
        repricedInvoice.setDiscount(newDiscount);
        repricedInvoice.setTotal(newTotal);
        repricedInvoice.setPaid(totalPaid >= newTotal - 0.01);

        Reservation updated = reservationRepository.modifyBooking(
                id, newCheckIn, newCheckOut, newRoomType, repricedInvoice);

        if (oldType != null) {
            roomAvailabilityPublisher.publish(oldType, oldCheckIn, oldCheckOut, "Reservation #" + id + " modified");
        }
        activityLogService.log(actor, "MODIFY", "Reservation", String.valueOf(id),
                "Updated to " + newCheckIn + " - " + newCheckOut + ", " + newRoomType
                        + "; total changed from $" + String.format("%.2f", oldTotal)
                        + " to $" + String.format("%.2f", newTotal));
        return updated;
    }

    private BookingInput pricingInputForEdit(Reservation reservation, LocalDate checkIn,
                                             LocalDate checkOut, RoomType roomType, int roomCount) {
        AdminBookingRequest request = new AdminBookingRequest();
        request.setAdults(reservation.getNumAdults());
        request.setChildren(reservation.getNumChildren());
        request.setCheckIn(checkIn);
        request.setCheckOut(checkOut);

        switch (roomType) {
            case SINGLE: request.setSingleQty(roomCount); break;
            case DOUBLE: request.setDoubleQty(roomCount); break;
            case DELUXE: request.setDeluxeQty(roomCount); break;
            case PENTHOUSE: request.setPenthouseQty(roomCount); break;
            default: throw new IllegalArgumentException("Room type is required.");
        }

        for (AddOn addOn : reservation.getAddOns()) {
            if (PricingConfig.WIFI_NAME.equals(addOn.getName())) request.setWifiSelected(true);
            if (PricingConfig.BREAKFAST_NAME.equals(addOn.getName())) request.setBreakfastSelected(true);
            if (PricingConfig.PARKING_NAME.equals(addOn.getName())) request.setParkingSelected(true);
            if (PricingConfig.SPA_NAME.equals(addOn.getName())) request.setSpaSelected(true);
        }
        return request;
    }

    /** Rooms of the given type/dates that are free, ignoring the reservation's own current room holds. */
    public long checkAvailability(RoomType type, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        return reservationRepository.countAvailableRooms(type, checkIn, checkOut, excludeReservationId);
    }

    public Reservation applyDiscount(Long id, double percent, String actor) {
        Reservation reservation = reservationRepository.findById(id);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation with ID " + id + " does not exist.");
        }
        Invoice invoice = reservation.getInvoice();
        double undiscounted = round(invoice.getSubtotal() + invoice.getTax());
        double discount = round(undiscounted * Math.max(0, percent));

        invoice.setDiscount(discount);
        invoice.setTotal(round(undiscounted - discount));
        Reservation saved = reservationRepository.save(reservation);

        activityLogService.log(actor, "DISCOUNT_APPLY", "Reservation", String.valueOf(id),
                String.format("%.0f%% discount applied (-$%.2f), new total $%.2f",
                        percent * 100, discount, invoice.getTotal()));
        return saved;
    }

    /**
     * Completes checkout: records any discount/loyalty reduction applied against the
     * invoice's original total, marks the reservation CHECKED_OUT, and frees its rooms
     * for subscribers (dashboard notifications, waitlist).
     */
    public void checkOutReservation(Long id, double finalTotal, String actor) {
        Reservation reservation = reservationRepository.findById(id);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation with ID " + id + " does not exist.");
        }
        if (reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            return;
        }
        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException(
                    "Only a checked-in reservation can be checked out (current status: " + reservation.getStatus() + ").");
        }

        Invoice invoice = reservation.getInvoice();
        double reduction = invoice.getTotal() - finalTotal;
        if (reduction > 0.001) {
            invoice.setDiscount(round(invoice.getDiscount() + reduction));
            invoice.setTotal(round(finalTotal));
        }
        invoice.setPaid(true);
        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.save(reservation);

        for (RoomType type : reservation.getRooms().stream().map(Room::getRoomType).distinct().toArray(RoomType[]::new)) {
            roomAvailabilityPublisher.publish(type, reservation.getCheckInDate(), reservation.getCheckOutDate(),
                    "Reservation #" + id + " checked out");
        }
        activityLogService.log(actor, "CHECKOUT", "Reservation", String.valueOf(id), "Checked out");
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
