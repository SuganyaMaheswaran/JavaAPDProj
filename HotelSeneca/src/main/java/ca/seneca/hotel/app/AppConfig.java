package ca.seneca.hotel.app;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.factory.RoomFactory;
import ca.seneca.hotel.repositories.IGuestRepository;
import ca.seneca.hotel.repositories.IReservationRepository;
import ca.seneca.hotel.repositories.IRoomRepository;
import ca.seneca.hotel.service.PricingService;
import ca.seneca.hotel.service.PricingStrategy;
import ca.seneca.hotel.service.ReservationService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Guice module: the central place that wires the application together.
 *
 * Every object is provided by {@link AppContext} rather than built here, so the
 * whole application shares one set of singletons. That matters because JavaFX
 * only routes the FIRST FXML through this injector — controllers opened later
 * by FXMLLoader.load() inside a scene switch are created by JavaFX itself and
 * read their collaborators straight from AppContext.
 */
public final class AppConfig extends AbstractModule {

    @Override
    protected void configure() {
        // Nothing to bind by type: everything is supplied by the @Provides
        // methods below so both wiring paths hand out the same instances.
    }

    @Provides @Singleton
    public RoomFactory providesRoomFactory() {
        return AppContext.roomFactory();
    }

    @Provides @Singleton
    public PricingStrategy providesPricingStrategy() {
        return AppContext.pricingStrategy();
    }

    @Provides @Singleton
    public IRoomRepository providesRoomRepository() {
        return AppContext.roomRepository();
    }

    @Provides @Singleton
    public IGuestRepository providesGuestRepository() {
        return AppContext.guestRepository();
    }

    @Provides @Singleton
    public IReservationRepository providesReservationRepository() {
        return AppContext.reservationRepository();
    }

    @Provides @Singleton
    public PricingService providesPricingService() {
        return AppContext.pricingService();
    }

    @Provides @Singleton
    public ReservationService providesReservationService() {
        return AppContext.reservationService();
    }
}
