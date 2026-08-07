package ca.seneca.hotel.models;

public enum Role {
    ADMIN("Admin"),
    MANAGER("Manager");

    private final String display;

    Role(String display) {
        this.display = display;
    }

    @Override
    public String toString() {
        return display;
    }
}
