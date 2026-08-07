package ca.seneca.hotel.events;

import ca.seneca.hotel.models.RoomType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Observer that keeps a bounded, UI-bindable feed of room-availability events so the
 * admin dashboard can show a live notification list without polling the database.
 */
public class NotificationCenter implements RoomAvailabilityObserver {

    private static final int MAX_ENTRIES = 50;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ObservableList<String> notifications = FXCollections.observableArrayList();

    public ObservableList<String> getNotifications() {
        return notifications;
    }

    @Override
    public void onRoomAvailable(RoomType roomType, LocalDate from, LocalDate to, String reason) {
        String message = "[" + LocalTime.now().format(TIME_FMT) + "] " + roomType + " room available "
                + from + " to " + to + " (" + reason + ")";
        notifications.add(0, message);
        while (notifications.size() > MAX_ENTRIES) {
            notifications.remove(notifications.size() - 1);
        }
    }
}
