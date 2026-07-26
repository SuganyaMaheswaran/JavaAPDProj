package ca.seneca.hotel.factory;

import ca.seneca.hotel.models.Room;
import ca.seneca.hotel.models.RoomType;

/**
 * Factory pattern: the single place that knows how to build a {@link Room}
 * with the attributes configured for its {@link RoomType}.
 *
 * Callers never call {@code new Room(...)} directly, so room numbering and
 * default availability stay consistent across the whole application.
 */
public class RoomFactory {

    /** Floor number used for each room type, e.g. a Deluxe becomes room 301. */
    private static int floorFor(RoomType type) {
        switch (type) {
            case SINGLE:    return 1;
            case DOUBLE:    return 2;
            case DELUXE:    return 3;
            case PENTHOUSE: return 4;
            default: throw new IllegalArgumentException("Unknown room type: " + type);
        }
    }

    /**
     * Creates a room of the given type.
     *
     * @param type    which kind of room to build
     * @param index   1-based position on that floor, used for the room number
     */
    public Room createRoom(RoomType type, int index) {
        if (type == null) {
            throw new IllegalArgumentException("Room type is required");
        }
        if (index < 1) {
            throw new IllegalArgumentException("Room index must be 1 or greater, got " + index);
        }

        Room room = new Room();
        room.setRoomType(type);
        room.setRoomNumber(String.valueOf(floorFor(type) * 100 + index));
        room.setAvailable(true);
        return room;
    }

    /**
     * Creates an unsaved room used only to read the configured price for a type
     * (see PricingService). It is never persisted.
     */
    public Room createPrototype(RoomType type) {
        Room room = new Room();
        room.setRoomType(type);
        room.setRoomNumber("PROTOTYPE-" + type.name());
        room.setAvailable(false);
        return room;
    }
}
