package ca.seneca.hotel.models;

import java.time.LocalDate;

public class KioskSession {
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
    private boolean enrolledLoyalty;

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
        enrolledLoyalty = false;
    }

    // Getters and Setters
    public int getAdults() { return adults; }
    public void setAdults(int adults) { this.adults = adults; }

    public int getChildren() { return children; }
    public void setChildren(int children) { this.children = children; }

    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }

    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }

    public int getSingleQty() { return singleQty; }
    public void setSingleQty(int singleQty) { this.singleQty = singleQty; }

    public int getDoubleQty() { return doubleQty; }
    public void setDoubleQty(int doubleQty) { this.doubleQty = doubleQty; }

    public int getDeluxeQty() { return deluxeQty; }
    public void setDeluxeQty(int deluxeQty) { this.deluxeQty = deluxeQty; }

    public int getPenthouseQty() { return penthouseQty; }
    public void setPenthouseQty(int penthouseQty) { this.penthouseQty = penthouseQty; }

    public boolean isWifiSelected() { return wifiSelected; }
    public void setWifiSelected(boolean wifiSelected) { this.wifiSelected = wifiSelected; }

    public boolean isBreakfastSelected() { return breakfastSelected; }
    public void setBreakfastSelected(boolean breakfastSelected) { this.breakfastSelected = breakfastSelected; }

    public boolean isParkingSelected() { return parkingSelected; }
    public void setParkingSelected(boolean parkingSelected) { this.parkingSelected = parkingSelected; }

    public boolean isSpaSelected() { return spaSelected; }
    public void setSpaSelected(boolean spaSelected) { this.spaSelected = spaSelected; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public boolean isEnrolledLoyalty() { return enrolledLoyalty; }
    public void setEnrolledLoyalty(boolean enrolledLoyalty) { this.enrolledLoyalty = enrolledLoyalty; }
}