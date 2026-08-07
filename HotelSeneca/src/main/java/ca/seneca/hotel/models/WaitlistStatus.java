package ca.seneca.hotel.models;

public enum WaitlistStatus {
    WAITING("Waiting"),
    CONVERTED("Converted"),
    EXPIRED("Expired");

    private final String display;

    WaitlistStatus(String display) {
        this.display = display;
    }

    @Override
    public String toString() {
        return display;
    }
}
