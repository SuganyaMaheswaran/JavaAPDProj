package ca.seneca.hotel.config;

import ca.seneca.hotel.factory.RoomFactory;
import ca.seneca.hotel.repositories.GuestRepository;
import ca.seneca.hotel.repositories.IGuestRepository;
import ca.seneca.hotel.repositories.IReservationRepository;
import ca.seneca.hotel.repositories.IRoomRepository;
import ca.seneca.hotel.repositories.JpaReservationRepository;
import ca.seneca.hotel.repositories.JpaRoomRepository;
import ca.seneca.hotel.service.DataSeeder;
import ca.seneca.hotel.service.PricingService;
import ca.seneca.hotel.service.PricingStrategy;
import ca.seneca.hotel.service.ReservationService;
import ca.seneca.hotel.service.WeekendPricingStrategy;

/**
 * Central wiring for the application. JavaFX creates controllers through their
 * no-argument constructor, so controllers pull their collaborators from here
 * instead of building them inline.
 *
 * Every object below is a lazily created singleton.
 */
public final class AppContext {

    private static IRoomRepository roomRepository;
    private static IGuestRepository guestRepository;
    private static IReservationRepository reservationRepository;
    private static RoomFactory roomFactory;
    private static PricingStrategy pricingStrategy;
    private static PricingService pricingService;
    private static ReservationService reservationService;

    private AppContext() {}

    public static synchronized RoomFactory roomFactory() {
        if (roomFactory == null) roomFactory = new RoomFactory();
        return roomFactory;
    }

    /**
     * Swap this for {@code new StandardPricingStrategy()} to charge a flat rate
     * every night instead of a weekend premium.
     */
    public static synchronized PricingStrategy pricingStrategy() {
        if (pricingStrategy == null) pricingStrategy = new WeekendPricingStrategy();
        return pricingStrategy;
    }

    public static synchronized IRoomRepository roomRepository() {
        if (roomRepository == null) roomRepository = new JpaRoomRepository();
        return roomRepository;
    }

    public static synchronized IGuestRepository guestRepository() {
        if (guestRepository == null) guestRepository = new GuestRepository();
        return guestRepository;
    }

    public static synchronized IReservationRepository reservationRepository() {
        if (reservationRepository == null) reservationRepository = new JpaReservationRepository();
        return reservationRepository;
    }

    public static synchronized PricingService pricingService() {
        if (pricingService == null) {
            pricingService = new PricingService(pricingStrategy(), roomFactory());
        }
        return pricingService;
    }

    public static synchronized ReservationService reservationService() {
        if (reservationService == null) {
            reservationService = new ReservationService(reservationRepository(), pricingService());
        }
        return reservationService;
    }

    /** Called once at startup to create the room inventory and add-on catalogue. */
    public static void seedDatabase() {
        new DataSeeder(roomRepository(), roomFactory()).seed();
    }
}
