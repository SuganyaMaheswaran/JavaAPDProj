package ca.seneca.hotel.models;

public enum RoomType {
    SINGLE("Single", 150.00, 2),
    DOUBLE("Double", 300.00, 4),
    DELUXE("Deluxe", 500.00, 2),
    PENTHOUSE("Penthouse", 900.00, 2);

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
