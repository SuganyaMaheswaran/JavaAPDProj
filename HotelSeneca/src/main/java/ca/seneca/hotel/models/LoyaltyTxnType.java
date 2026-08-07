package ca.seneca.hotel.models;

public enum LoyaltyTxnType {
    EARN("Earn"),
    REDEEM("Redeem");

    private final String display;

    LoyaltyTxnType(String display) {
        this.display = display;
    }

    @Override
    public String toString() {
        return display;
    }
}
