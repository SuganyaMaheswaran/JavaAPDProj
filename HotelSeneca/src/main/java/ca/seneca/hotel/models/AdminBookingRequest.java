package ca.seneca.hotel.models;

import java.time.LocalDate;

/** Plain data holder for a reservation an admin creates on the guest's behalf (e.g. over the phone). */
public class AdminBookingRequest implements BookingInput {

    private int adults = 1;
    private int children = 0;
    private LocalDate checkIn;
    private LocalDate checkOut;

    private int singleQty;
    private int doubleQty;
    private int deluxeQty;
    private int penthouseQty;

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
    private boolean enrollRequested;

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
