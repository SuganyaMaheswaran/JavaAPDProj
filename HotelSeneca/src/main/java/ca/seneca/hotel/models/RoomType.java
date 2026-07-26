package ca.seneca.hotel.models;

public enum RoomType {
    // Base prices must match what the kiosk shows in kiosk_room_plan_view.fxml
    SINGLE("Single", 119.00, 2),
    DOUBLE("Double", 189.00, 4),
    DELUXE("Deluxe", 259.00, 2),
    PENTHOUSE("Penthouse", 429.00, 2);

    private final String displayName;
    private final Double basePrice;
    private final int maxOccupancy;

    RoomType(String displayName, Double basePrice, int maxOccupancy) {
        this.displayName = displayName;
        this.basePrice = basePrice;
        this.maxOccupancy = maxOccupancy;
    }

    public Double getBasePrice() {
        return basePrice;
    }
    public int getMaxOccupancy() {
        return maxOccupancy;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
