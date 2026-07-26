package ca.seneca.hotel.models;

import java.time.LocalDate;

public class KioskSession {
    private static final KioskSession INSTANCE = new KioskSession();

    private int adults = 1;
    private int children = 0;
    private LocalDate checkIn;
    private LocalDate checkOut;
    
    private String roomType = "Double";
    private int roomQuantity = 1;

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
        roomType = "Double";
        roomQuantity = 1;
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

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public int getRoomQuantity() { return roomQuantity; }
    public void setRoomQuantity(int roomQuantity) { this.roomQuantity = roomQuantity; }

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