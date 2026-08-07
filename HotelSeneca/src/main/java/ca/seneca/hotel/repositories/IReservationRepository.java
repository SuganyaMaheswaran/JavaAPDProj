package ca.seneca.hotel.repositories;

import ca.seneca.hotel.models.Guest;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.models.RoomType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface IReservationRepository {

    Reservation save(Reservation reservation);

    Reservation findById(Long id);

    List<Reservation> findAll();

    boolean existsById(Long id);

    void deleteById(Long id);

    /**
     * Persists a whole booking in a single transaction: the guest, the rooms
     * allocated for the requested dates, the add-ons and the invoice.
     *
     * @param addOnNames add-on names as defined in PricingConfig
     * @throws IllegalStateException if not enough rooms of a requested type are free
     */
    Reservation createBooking(Guest guest,
                              Reservation reservation,
                              Map<RoomType, Integer> roomsNeeded,
                              List<String> addOnNames);

    /**
     * Releases the reservation's current rooms and reallocates the same quantity
     * against the new room type/dates.
     *
     * @throws IllegalStateException if not enough rooms of the new type are free
     */
    Reservation modifyBooking(Long reservationId, LocalDate newCheckIn, LocalDate newCheckOut, RoomType newRoomType);

    /** Free rooms of the given type/dates, ignoring the given reservation's own room holds. */
    long countAvailableRooms(RoomType type, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId);
}
