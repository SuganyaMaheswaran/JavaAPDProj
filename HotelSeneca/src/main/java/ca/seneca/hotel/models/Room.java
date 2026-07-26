package ca.seneca.hotel.models;

import javax.persistence.*;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name="rooms", uniqueConstraints = {@UniqueConstraint(columnNames = "roomNumber")})
public class Room implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false, length = 50)
    private String roomNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 200)
    private RoomType roomType;
    @Column(nullable = false)
    private boolean available;

    public Room() {
    }

    public Room(long id, String roomNumber, RoomType roomType, boolean available) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.available = available;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public double getBasePrice() {
        return roomType.getBasePrice();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return Objects.equals(getRoomNumber(), room.getRoomNumber()) && Objects.equals(getRoomType(), room.getRoomType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRoomNumber(), getRoomType());
    }

    @Override
    public String toString() {
        return "Room{" +
                "roomNumber=" + roomNumber +
                ", roomType='" + roomType + '\'' +
                ", available=" + available +
                '}';
    }
}
