package ca.seneca.hotel.models;

public enum ReservationStatus {
    BOOKED("Booked"),
    CHECKED_IN("Checked In"),
    CHECKED_OUT("Checked Out"),
    CANCELLED("Cancelled");

    private final String display;
    ReservationStatus(String d) {
        this.display = d;
    }
    @Override public String toString() {
        return display;
    }
}
