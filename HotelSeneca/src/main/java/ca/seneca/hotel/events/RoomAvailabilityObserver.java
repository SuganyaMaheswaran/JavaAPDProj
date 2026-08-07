package ca.seneca.hotel.events;

import ca.seneca.hotel.models.RoomType;

import java.time.LocalDate;

/** Observer role: notified whenever rooms become available again (cancellation or checkout). */
@FunctionalInterface
public interface RoomAvailabilityObserver {
    void onRoomAvailable(RoomType roomType, LocalDate from, LocalDate to, String reason);
}
