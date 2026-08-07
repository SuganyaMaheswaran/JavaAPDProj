package ca.seneca.hotel.models;

public enum PaymentMethod {
    CASH("Cash"),
    CARD("Card"),
    LOYALTY_POINTS("Loyalty Points");

    private final String display;

    PaymentMethod(String display) {
        this.display = display;
    }

    @Override
    public String toString() {
        return display;
    }
}
