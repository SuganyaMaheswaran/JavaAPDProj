package ca.seneca.hotel.app;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.events.NotificationCenter;
import ca.seneca.hotel.events.RoomAvailabilityPublisher;
import ca.seneca.hotel.factory.RoomFactory;
import ca.seneca.hotel.repositories.IActivityLogRepository;
import ca.seneca.hotel.repositories.IAdminUserRepository;
import ca.seneca.hotel.repositories.IFeedbackRepository;
import ca.seneca.hotel.repositories.IGuestRepository;
import ca.seneca.hotel.repositories.ILoyaltyTransactionRepository;
import ca.seneca.hotel.repositories.IPaymentRepository;
import ca.seneca.hotel.repositories.IReservationRepository;
import ca.seneca.hotel.repositories.IRoomRepository;
import ca.seneca.hotel.repositories.IWaitlistRepository;
import ca.seneca.hotel.security.AuthService;
import ca.seneca.hotel.service.ActivityLogService;
import ca.seneca.hotel.service.FeedbackService;
import ca.seneca.hotel.service.LoyaltyService;
import ca.seneca.hotel.service.PaymentService;
import ca.seneca.hotel.service.PricingService;
import ca.seneca.hotel.service.PricingStrategy;
import ca.seneca.hotel.service.ReportingService;
import ca.seneca.hotel.service.ReservationService;
import ca.seneca.hotel.service.WaitlistService;
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
    public IAdminUserRepository providesAdminUserRepository() {
        return AppContext.adminUserRepository();
    }

    @Provides @Singleton
    public IPaymentRepository providesPaymentRepository() {
        return AppContext.paymentRepository();
    }

    @Provides @Singleton
    public ILoyaltyTransactionRepository providesLoyaltyTransactionRepository() {
        return AppContext.loyaltyTransactionRepository();
    }

    @Provides @Singleton
    public IWaitlistRepository providesWaitlistRepository() {
        return AppContext.waitlistRepository();
    }

    @Provides @Singleton
    public IFeedbackRepository providesFeedbackRepository() {
        return AppContext.feedbackRepository();
    }

    @Provides @Singleton
    public IActivityLogRepository providesActivityLogRepository() {
        return AppContext.activityLogRepository();
    }

    @Provides @Singleton
    public PricingService providesPricingService() {
        return AppContext.pricingService();
    }

    @Provides @Singleton
    public ReservationService providesReservationService() {
        return AppContext.reservationService();
    }

    @Provides @Singleton
    public ActivityLogService providesActivityLogService() {
        return AppContext.activityLogService();
    }

    @Provides @Singleton
    public AuthService providesAuthService() {
        return AppContext.authService();
    }

    @Provides @Singleton
    public PaymentService providesPaymentService() {
        return AppContext.paymentService();
    }

    @Provides @Singleton
    public LoyaltyService providesLoyaltyService() {
        return AppContext.loyaltyService();
    }

    @Provides @Singleton
    public WaitlistService providesWaitlistService() {
        return AppContext.waitlistService();
    }

    @Provides @Singleton
    public FeedbackService providesFeedbackService() {
        return AppContext.feedbackService();
    }

    @Provides @Singleton
    public ReportingService providesReportingService() {
        return AppContext.reportingService();
    }

    @Provides @Singleton
    public RoomAvailabilityPublisher providesRoomAvailabilityPublisher() {
        return AppContext.roomAvailabilityPublisher();
    }

    @Provides @Singleton
    public NotificationCenter providesNotificationCenter() {
        return AppContext.notificationCenter();
    }
}
