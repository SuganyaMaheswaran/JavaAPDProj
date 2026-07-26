package ca.seneca.hotel.models;

import javax.persistence.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name="guests", uniqueConstraints = {@UniqueConstraint(columnNames = "email")})
public class Guest implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50)
    private String firstName;
    @Column(nullable = false, length = 50)
    private String lastName;
    @Column(nullable = false, length = 200)
    private String phone;
    @Column(nullable = false, unique = true, length = 200)
    private String email;
    @Column(nullable = false, length = 600)
    private String address;
    @Column(nullable = false, length = 200)
    private String city;
    @Column(nullable = false, length = 200)
    private String postalCode;
    @Column(nullable = false)
    private Boolean isLoyaltyMember;

    public Guest() {
    }

    public Guest(Long id, String firstName, String lastName, String phone, String email, String address, String city, String postalCode, Boolean isLoyaltyMember) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.city = city;
        this.postalCode = postalCode;
        this.isLoyaltyMember = isLoyaltyMember;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public Boolean getLoyaltyMember() {
        return isLoyaltyMember;
    }

    public void setLoyaltyMember(Boolean loyaltyMember) {
        isLoyaltyMember = loyaltyMember;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Guest guest = (Guest) o;
        return Objects.equals(getId(), guest.getId()) && Objects.equals(getFirstName(), guest.getFirstName()) && Objects.equals(getLastName(), guest.getLastName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getFirstName(), getLastName());
    }

    @Override
    public String toString() {
        return "Guest{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", address='" + address + '\'' +
                ", city='" + city + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", isLoyaltyMember=" + isLoyaltyMember +
                '}';
    }
}
