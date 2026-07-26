package ca.seneca.hotel.repositories;

import ca.seneca.hotel.models.Room;
import ca.seneca.hotel.models.RoomType;
import ca.seneca.hotel.util.JpaUtil;

import java.util.List;

public class JpaRoomRepository implements IRoomRepository {

    @Override
    public Room save(Room room) {
        return JpaUtil.runInTransactionReturning(em -> {
            if (room.getId() == 0L) {
                em.persist(room);
                return room;
            }
            return em.merge(room);
        });
    }

    @Override
    public List<Room> findAll() {
        return JpaUtil.runInTransactionReturning(em ->
                em.createQuery("SELECT r FROM Room r ORDER BY r.roomNumber", Room.class)
                        .getResultList());
    }

    @Override
    public long count() {
        return JpaUtil.runInTransactionReturning(em ->
                em.createQuery("SELECT COUNT(r) FROM Room r", Long.class)
                        .getSingleResult());
    }

    @Override
    public List<Room> findAvailableByType(RoomType type) {
        return JpaUtil.runInTransactionReturning(em ->
                em.createQuery(
                                "SELECT r FROM Room r WHERE r.roomType = :type AND r.available = true "
                                        + "ORDER BY r.roomNumber", Room.class)
                        .setParameter("type", type)
                        .getResultList());
    }
}
