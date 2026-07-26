package ca.seneca.hotel.app;

import ca.seneca.hotel.repositories.GuestRepository;
import ca.seneca.hotel.repositories.IGuestRepository;
import ca.seneca.hotel.repositories.JpaReservationRepository;
import ca.seneca.hotel.service.ReservationService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

// Guice Base Configuration Class
public final class AppConfig extends AbstractModule {

    // Reservation binding
    @Override
    protected void configure() {
        bind(IGuestRepository.class).to(GuestRepository.class).in(Singleton.class);
        bind(JpaReservationRepository.class).in(Singleton.class);
    }

    // provider methods
    @Provides
    @Singleton
    public ReservationService providesReservationService(JpaReservationRepository reservationRepository) {
        return new ReservationService(reservationRepository);
    }
}