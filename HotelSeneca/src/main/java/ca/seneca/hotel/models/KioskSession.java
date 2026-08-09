package ca.seneca.hotel.models;

import java.time.LocalDate;

public class KioskSession implements BookingInput {
    private static final KioskSession INSTANCE = new KioskSession();

    private int adults = 1;
    private int children = 0;
    private LocalDate checkIn;
    private LocalDate checkOut;
    
    // Individual Room Quantities. All start at 0 so KioskRoomSelectionController
    // can tell "nothing chosen yet" apart from a real choice and suggest a plan.
    private int singleQty = 0;
    private int doubleQty = 0;
    private int deluxeQty = 0;
    private int penthouseQty = 0;

    /** True once the guest opts out of the suggested plan and edits quantities. */
    private boolean chooseOwnRooms = false;

    /**
     * Lets the room screen notice that the guest went back and changed the head count, and
     * re-suggest instead of showing a plan that no longer fits.
     */
    private int suggestedForGuests = -1;

    private boolean wifiSelected;
    private boolean breakfastSelected;
    private boolean parkingSelected;
    private boolean spaSelected;

    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String address;
    private String city;
    private String postalCode;
    private boolean existingMember;
    // if the guest ticked "enroll me"; they are not a member until this booking saves.
    private boolean enrollRequested;

    private KioskSession() {}

    public static KioskSession getInstance() {
        return INSTANCE;
    }

    public void reset() {
        adults = 1;
        children = 0;
        checkIn = null;
        checkOut = null;
        singleQty = 0;
        doubleQty = 0;
        deluxeQty = 0;
        penthouseQty = 0;
        chooseOwnRooms = false;
        suggestedForGuests = -1;
        wifiSelected = false;
        breakfastSelected = false;
        parkingSelected = false;
        spaSelected = false;
        firstName = null;
        lastName = null;
        phone = null;
        email = null;
        address = null;
        city = null;
        postalCode = null;
        existingMember = false;
        enrollRequested = false;
    }

    // Getters and Setters
    @Override public int getAdults() { return adults; }
    public void setAdults(int adults) { this.adults = adults; }

    @Override public int getChildren() { return children; }
    public void setChildren(int children) { this.children = children; }

    @Override public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }

    @Override public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }

    @Override public int getSingleQty() { return singleQty; }
    public void setSingleQty(int singleQty) { this.singleQty = singleQty; }

    @Override public int getDoubleQty() { return doubleQty; }
    public void setDoubleQty(int doubleQty) { this.doubleQty = doubleQty; }

    @Override public int getDeluxeQty() { return deluxeQty; }
    public void setDeluxeQty(int deluxeQty) { this.deluxeQty = deluxeQty; }

    @Override public int getPenthouseQty() { return penthouseQty; }
    public void setPenthouseQty(int penthouseQty) { this.penthouseQty = penthouseQty; }

    public int getSuggestedForGuests() { return suggestedForGuests; }
    public void setSuggestedForGuests(int suggestedForGuests) { this.suggestedForGuests = suggestedForGuests; }

    public boolean isChooseOwnRooms() { return chooseOwnRooms; }
    public void setChooseOwnRooms(boolean chooseOwnRooms) { this.chooseOwnRooms = chooseOwnRooms; }

    @Override public boolean isWifiSelected() { return wifiSelected; }
    public void setWifiSelected(boolean wifiSelected) { this.wifiSelected = wifiSelected; }

    @Override public boolean isBreakfastSelected() { return breakfastSelected; }
    public void setBreakfastSelected(boolean breakfastSelected) { this.breakfastSelected = breakfastSelected; }

    @Override public boolean isParkingSelected() { return parkingSelected; }
    public void setParkingSelected(boolean parkingSelected) { this.parkingSelected = parkingSelected; }

    @Override public boolean isSpaSelected() { return spaSelected; }
    public void setSpaSelected(boolean spaSelected) { this.spaSelected = spaSelected; }

    @Override public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    @Override public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    @Override public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    @Override public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    @Override public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    @Override public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    @Override public boolean isExistingMember() { return existingMember; }
    public void setExistingMember(boolean existingMember) { this.existingMember = existingMember; }

    @Override public boolean isEnrollRequested() { return enrollRequested; }
    public void setEnrollRequested(boolean enrollRequested) { this.enrollRequested = enrollRequested; }
}