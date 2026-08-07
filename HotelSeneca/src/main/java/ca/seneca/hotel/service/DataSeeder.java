package ca.seneca.hotel.service;

import ca.seneca.hotel.config.PricingConfig;
import ca.seneca.hotel.factory.RoomFactory;
import ca.seneca.hotel.models.AddOn;
import ca.seneca.hotel.models.AdminUser;
import ca.seneca.hotel.models.PricingModel;
import ca.seneca.hotel.models.Role;
import ca.seneca.hotel.models.Room;
import ca.seneca.hotel.models.RoomType;
import ca.seneca.hotel.repositories.IAdminUserRepository;
import ca.seneca.hotel.repositories.IRoomRepository;
import ca.seneca.hotel.security.BCryptPasswordHasher;
import ca.seneca.hotel.util.JpaUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Fills the database with the hotel's room inventory and the add-on catalogue
 * the first time the application starts. Safe to call on every startup: it does
 * nothing once the rows exist.
 */
public class DataSeeder {

    private static final Logger logger = Logger.getLogger(DataSeeder.class.getName());

    /** How many rooms of each type this hotel has. */
    private static final Map<RoomType, Integer> INVENTORY = new LinkedHashMap<>();
    static {
        INVENTORY.put(RoomType.SINGLE, 10);
        INVENTORY.put(RoomType.DOUBLE, 10);
        INVENTORY.put(RoomType.DELUXE, 5);
        INVENTORY.put(RoomType.PENTHOUSE, 3);
    }

    /** Demo credentials printed to the console so the assignment can be graded without shared secrets. */
    private static final String DEMO_ADMIN_PASSWORD = "admin123";
    private static final String DEMO_MANAGER_PASSWORD = "manager123";

    private final IRoomRepository roomRepository;
    private final RoomFactory roomFactory;
    private final IAdminUserRepository adminUserRepository;

    // Constructor-based dependency injection
    public DataSeeder(IRoomRepository roomRepository, RoomFactory roomFactory,
                      IAdminUserRepository adminUserRepository) {
        this.roomRepository = roomRepository;
        this.roomFactory = roomFactory;
        this.adminUserRepository = adminUserRepository;
    }

    public void seed() {
        seedRooms();
        seedAddOns();
        seedAdminUsers();
    }

    private void seedRooms() {
        if (roomRepository.count() > 0) {
            logger.info("Room inventory already present, skipping seed.");
            return;
        }

        int created = 0;
        for (Map.Entry<RoomType, Integer> entry : INVENTORY.entrySet()) {
            for (int i = 1; i <= entry.getValue(); i++) {
                // Factory pattern: rooms are only ever built here.
                Room room = roomFactory.createRoom(entry.getKey(), i);
                roomRepository.save(room);
                created++;
            }
        }
        logger.info("Seeded " + created + " rooms.");
    }

    private void seedAddOns() {
        long existing = JpaUtil.runInTransactionReturning(em ->
                em.createQuery("SELECT COUNT(a) FROM AddOn a", Long.class).getSingleResult());

        if (existing > 0) {
            logger.info("Add-on catalogue already present, skipping seed.");
            return;
        }

        JpaUtil.executeInTransaction(em -> {
            em.persist(newAddOn(PricingConfig.WIFI_NAME, PricingConfig.WIFI_PRICE, PricingConfig.WIFI_MODEL));
            em.persist(newAddOn(PricingConfig.BREAKFAST_NAME, PricingConfig.BREAKFAST_PRICE, PricingConfig.BREAKFAST_MODEL));
            em.persist(newAddOn(PricingConfig.PARKING_NAME, PricingConfig.PARKING_PRICE, PricingConfig.PARKING_MODEL));
            em.persist(newAddOn(PricingConfig.SPA_NAME, PricingConfig.SPA_PRICE, PricingConfig.SPA_MODEL));
        });
        logger.info("Seeded 4 add-ons.");
    }

    private void seedAdminUsers() {
        if (adminUserRepository.count() > 0) {
            logger.info("Admin accounts already present, skipping seed.");
            return;
        }

        adminUserRepository.save(new AdminUser("admin", BCryptPasswordHasher.hash(DEMO_ADMIN_PASSWORD), Role.ADMIN));
        adminUserRepository.save(new AdminUser("manager", BCryptPasswordHasher.hash(DEMO_MANAGER_PASSWORD), Role.MANAGER));
        logger.info("Seeded demo admin accounts: admin/" + DEMO_ADMIN_PASSWORD + ", manager/" + DEMO_MANAGER_PASSWORD);
    }

    private AddOn newAddOn(String name, double price, PricingModel model) {
        AddOn addOn = new AddOn();
        addOn.setName(name);
        addOn.setPrice(price);
        addOn.setPricingModel(model);
        return addOn;
    }
}
