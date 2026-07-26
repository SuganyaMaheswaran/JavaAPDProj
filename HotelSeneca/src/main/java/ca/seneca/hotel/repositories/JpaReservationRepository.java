package ca.seneca.hotel.repositories;

import ca.seneca.hotel.models.AddOn;
import ca.seneca.hotel.models.Guest;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.models.Room;
import ca.seneca.hotel.models.RoomType;
import ca.seneca.hotel.util.JpaUtil;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class JpaReservationRepository implements IReservationRepository {

    @Override
    public Reservation save(Reservation reservation) {
        return JpaUtil.runInTransactionReturning(em -> {
            if (reservation.getId() == null) {
                em.persist(reservation);
                return reservation;
            }
            return em.merge(reservation);
        });
    }

    @Override
    public Reservation findById(Long id) {
        return JpaUtil.runInTransactionReturning(em -> em.find(Reservation.class, id));
    }

    @Override
    public List<Reservation> findAll() {
        return JpaUtil.runInTransactionReturning(em ->
            em.createQuery("SELECT r FROM Reservation r", Reservation.class).getResultList()
        );
    }

    @Override
    public boolean existsById(Long id) {
        return findById(id) != null;
    }

    @Override
    public void deleteById(Long id) {
        JpaUtil.executeInTransaction(em -> {
            Reservation reservation = em.find(Reservation.class, id);
            if (reservation != null) {
                em.remove(reservation);
            }
        });
    }

    @Override
    public Reservation createBooking(Guest guest,
                                     Reservation reservation,
                                     Map<RoomType, Integer> roomsNeeded,
                                     List<String> addOnNames) {

        return JpaUtil.runInTransactionReturning(em -> {
            // 1. The guest. Reuse the existing record if this email booked before,
            //    because Guest.email carries a unique constraint.
            Guest managedGuest = em.createQuery(
                            "SELECT g FROM Guest g WHERE g.email = :email", Guest.class)
                    .setParameter("email", guest.getEmail())
                    .getResultStream()
                    .findFirst()
                    .orElse(null);

            if (managedGuest == null) {
                em.persist(guest);
                managedGuest = guest;
            } else {
                // Keep whatever the guest just typed in.
                managedGuest.setFirstName(guest.getFirstName());
                managedGuest.setLastName(guest.getLastName());
                managedGuest.setPhone(guest.getPhone());
                managedGuest.setAddress(guest.getAddress());
                managedGuest.setCity(guest.getCity());
                managedGuest.setPostalCode(guest.getPostalCode());
                managedGuest.setLoyaltyMember(guest.getLoyaltyMember());
            }
            reservation.setGuest(managedGuest);

            // 2. Allocate rooms that are free for these dates.
            for (Map.Entry<RoomType, Integer> entry : roomsNeeded.entrySet()) {
                RoomType type = entry.getKey();
                int qty = entry.getValue();

                List<Room> free = findFreeRooms(em, type,
                        reservation.getCheckInDate(), reservation.getCheckOutDate(), qty);

                if (free.size() < qty) {
                    throw new IllegalStateException(
                            "Only " + free.size() + " " + type + " room(s) are available for these dates, "
                                    + qty + " were requested.");
                }
                free.forEach(reservation::addRoom);
            }

            // 3. Link the selected add-ons (reference rows seeded at startup).
            for (String name : addOnNames) {
                em.createQuery("SELECT a FROM AddOn a WHERE a.name = :name", AddOn.class)
                        .setParameter("name", name)
                        .getResultStream()
                        .findFirst()
                        .ifPresent(reservation::addAddOn);
            }

            // 4. The invoice cascades from the reservation (CascadeType.ALL).
            em.persist(reservation);
            return reservation;
        });
    }

    /**
     * Rooms of the given type with no overlapping, non-cancelled reservation.
     */
    private List<Room> findFreeRooms(EntityManager em, RoomType type,
                                     LocalDate checkIn, LocalDate checkOut, int limit) {
        return em.createQuery(
                        "SELECT r FROM Room r "
                                + "WHERE r.roomType = :type AND r.available = true "
                                + "AND r.id NOT IN ("
                                + "  SELECT booked.id FROM Reservation res JOIN res.rooms booked "
                                + "  WHERE res.status <> ca.seneca.hotel.models.ReservationStatus.CANCELLED "
                                + "    AND res.checkInDate < :checkOut "
                                + "    AND res.checkOutDate > :checkIn"
                                + ") "
                                + "ORDER BY r.roomNumber", Room.class)
                .setParameter("type", type)
                .setParameter("checkIn", checkIn)
                .setParameter("checkOut", checkOut)
                .setMaxResults(limit)
                .getResultList();
    }
}
