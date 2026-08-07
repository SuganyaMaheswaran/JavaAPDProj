package ca.seneca.hotel.events;

import ca.seneca.hotel.models.RoomType;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Subject role of the Observer pattern: fans out room-availability events to every subscriber. */
public class RoomAvailabilityPublisher {

    private final List<RoomAvailabilityObserver> observers = new CopyOnWriteArrayList<>();

    public void subscribe(RoomAvailabilityObserver observer) {
        observers.add(observer);
    }

    public void unsubscribe(RoomAvailabilityObserver observer) {
        observers.remove(observer);
    }

    public void publish(RoomType roomType, LocalDate from, LocalDate to, String reason) {
        for (RoomAvailabilityObserver observer : observers) {
            observer.onRoomAvailable(roomType, from, to, reason);
        }
    }
}
