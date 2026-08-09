package ca.seneca.hotel.models;

import java.time.LocalDate;

/**
 * Contract shared by every source of a room booking request (the kiosk's singleton
 * {@link KioskSession} and admin phone-in bookings via {@link AdminBookingRequest}), so
 * {@code PricingService}/{@code ReservationService} can price and persist either one
 * through the same tested code path.
 */
public interface BookingInput {
    int getAdults();
    int getChildren();
    LocalDate getCheckIn();
    LocalDate getCheckOut();

    int getSingleQty();
    int getDoubleQty();
    int getDeluxeQty();
    int getPenthouseQty();

    boolean isWifiSelected();
    boolean isBreakfastSelected();
    boolean isParkingSelected();
    boolean isSpaSelected();

    String getFirstName();
    String getLastName();
    String getPhone();
    String getEmail();
    String getAddress();
    String getCity();
    String getPostalCode();

    boolean isExistingMember();
    // True when the guest asked to join the programme as part of this booking.
    boolean isEnrollRequested();
}
