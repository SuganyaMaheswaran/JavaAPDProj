package ca.seneca.hotel.repositories;

import ca.seneca.hotel.models.Room;
import ca.seneca.hotel.models.RoomType;

import java.util.List;

public interface IRoomRepository {

    Room save(Room room);

    List<Room> findAll();

    long count();

    /** Rooms of the given type that are currently marked available. */
    List<Room> findAvailableByType(RoomType type);
}
