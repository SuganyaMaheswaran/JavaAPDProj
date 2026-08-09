package ca.seneca.hotel.config;

import ca.seneca.hotel.events.NotificationCenter;
import ca.seneca.hotel.events.RoomAvailabilityPublisher;
import ca.seneca.hotel.factory.RoomFactory;
import ca.seneca.hotel.repositories.GuestRepository;
import ca.seneca.hotel.repositories.IActivityLogRepository;
import ca.seneca.hotel.repositories.IAdminUserRepository;
import ca.seneca.hotel.repositories.IFeedbackRepository;
import ca.seneca.hotel.repositories.IGuestRepository;
import ca.seneca.hotel.repositories.ILoyaltyTransactionRepository;
import ca.seneca.hotel.repositories.IPaymentRepository;
import ca.seneca.hotel.repositories.IReservationRepository;
import ca.seneca.hotel.repositories.IRoomRepository;
import ca.seneca.hotel.repositories.IWaitlistRepository;
import ca.seneca.hotel.repositories.JpaActivityLogRepository;
import ca.seneca.hotel.repositories.JpaAdminUserRepository;
import ca.seneca.hotel.repositories.JpaFeedbackRepository;
import ca.seneca.hotel.repositories.JpaLoyaltyTransactionRepository;
import ca.seneca.hotel.repositories.JpaPaymentRepository;
import ca.seneca.hotel.repositories.JpaReservationRepository;
import ca.seneca.hotel.repositories.JpaRoomRepository;
import ca.seneca.hotel.repositories.JpaWaitlistRepository;
import ca.seneca.hotel.security.AuthService;
import ca.seneca.hotel.service.ActivityLogService;
import ca.seneca.hotel.service.DataSeeder;
import ca.seneca.hotel.service.FeedbackService;
import ca.seneca.hotel.service.LoyaltyService;
import ca.seneca.hotel.service.PaymentService;
import ca.seneca.hotel.service.PricingService;
import ca.seneca.hotel.service.PricingStrategy;
import ca.seneca.hotel.service.ReportingService;
import ca.seneca.hotel.service.ReservationService;
import ca.seneca.hotel.service.WaitlistService;
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
    private static IAdminUserRepository adminUserRepository;
    private static IPaymentRepository paymentRepository;
    private static ILoyaltyTransactionRepository loyaltyTransactionRepository;
    private static IWaitlistRepository waitlistRepository;
    private static IFeedbackRepository feedbackRepository;
    private static IActivityLogRepository activityLogRepository;

    private static RoomFactory roomFactory;
    private static PricingStrategy pricingStrategy;
    private static PricingService pricingService;
    private static ReservationService reservationService;

    private static AuthService authService;
    private static ActivityLogService activityLogService;
    private static PaymentService paymentService;
    private static LoyaltyService loyaltyService;
    private static WaitlistService waitlistService;
    private static FeedbackService feedbackService;
    private static ReportingService reportingService;

    private static RoomAvailabilityPublisher roomAvailabilityPublisher;
    private static NotificationCenter notificationCenter;

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

    public static synchronized IAdminUserRepository adminUserRepository() {
        if (adminUserRepository == null) adminUserRepository = new JpaAdminUserRepository();
        return adminUserRepository;
    }

    public static synchronized IPaymentRepository paymentRepository() {
        if (paymentRepository == null) paymentRepository = new JpaPaymentRepository();
        return paymentRepository;
    }

    public static synchronized ILoyaltyTransactionRepository loyaltyTransactionRepository() {
        if (loyaltyTransactionRepository == null) loyaltyTransactionRepository = new JpaLoyaltyTransactionRepository();
        return loyaltyTransactionRepository;
    }

    public static synchronized IWaitlistRepository waitlistRepository() {
        if (waitlistRepository == null) waitlistRepository = new JpaWaitlistRepository();
        return waitlistRepository;
    }

    public static synchronized IFeedbackRepository feedbackRepository() {
        if (feedbackRepository == null) feedbackRepository = new JpaFeedbackRepository();
        return feedbackRepository;
    }

    public static synchronized IActivityLogRepository activityLogRepository() {
        if (activityLogRepository == null) activityLogRepository = new JpaActivityLogRepository();
        return activityLogRepository;
    }

    public static synchronized PricingService pricingService() {
        if (pricingService == null) {
            pricingService = new PricingService(pricingStrategy(), roomFactory());
        }
        return pricingService;
    }

    public static synchronized ReservationService reservationService() {
        if (reservationService == null) {
            reservationService = new ReservationService(
                    reservationRepository(), pricingService(), roomAvailabilityPublisher(),
                    activityLogService(), loyaltyService(), paymentRepository());
        }
        return reservationService;
    }

    public static synchronized ActivityLogService activityLogService() {
        if (activityLogService == null) {
            activityLogService = new ActivityLogService(activityLogRepository());
        }
        return activityLogService;
    }

    public static synchronized AuthService authService() {
        if (authService == null) {
            authService = new AuthService(adminUserRepository(), activityLogService());
        }
        return authService;
    }

    public static synchronized RoomAvailabilityPublisher roomAvailabilityPublisher() {
        if (roomAvailabilityPublisher == null) {
            roomAvailabilityPublisher = new RoomAvailabilityPublisher();
            roomAvailabilityPublisher.subscribe(notificationCenter());
            roomAvailabilityPublisher.subscribe(waitlistService());
        }
        return roomAvailabilityPublisher;
    }

    public static synchronized NotificationCenter notificationCenter() {
        if (notificationCenter == null) notificationCenter = new NotificationCenter();
        return notificationCenter;
    }

    public static synchronized PaymentService paymentService() {
        if (paymentService == null) {
            paymentService = new PaymentService(paymentRepository(), reservationRepository(), loyaltyService(), activityLogService());
        }
        return paymentService;
    }

    public static synchronized LoyaltyService loyaltyService() {
        if (loyaltyService == null) {
            loyaltyService = new LoyaltyService(guestRepository(), loyaltyTransactionRepository(), activityLogService());
        }
        return loyaltyService;
    }

    public static synchronized WaitlistService waitlistService() {
        if (waitlistService == null) {
            waitlistService = new WaitlistService(waitlistRepository(), activityLogService());
        }
        return waitlistService;
    }

    public static synchronized FeedbackService feedbackService() {
        if (feedbackService == null) {
            feedbackService = new FeedbackService(feedbackRepository(), activityLogService());
        }
        return feedbackService;
    }

    public static synchronized ReportingService reportingService() {
        if (reportingService == null) {
            reportingService = new ReportingService(reservationRepository(), roomRepository(), paymentRepository());
        }
        return reportingService;
    }

    /** Called once at startup to create the room inventory, add-on catalogue and demo admin accounts. */
    public static void seedDatabase() {
        new DataSeeder(roomRepository(), roomFactory(), adminUserRepository()).seed();
        // Force-create the Observer graph on startup so subscribers are in place before anything happens.
        roomAvailabilityPublisher();
    }
}
