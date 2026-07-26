package ca.seneca.hotel.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * STREAMING_CHUNK:Initializing Singleton EntityManagerFactory...
 * Provides centralized management for JPA operations.
 */
public class JpaUtil {
    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("HotelPU");

    private JpaUtil() {} // Prevent instantiation

    public static EntityManager createEntityManager() {
        return emf.createEntityManager();
    }

    /**
     * STREAMING_CHUNK:Defining transactional execution helper...
     */
    public static void executeInTransaction(Consumer<EntityManager> action) {
        EntityManager em = createEntityManager();
        try {
            em.getTransaction().begin();
            action.accept(em);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close(); 
        }
    }

    /**
     * STREAMING_CHUNK:Defining transactional query helper...
     */
    public static <T> T runInTransactionReturning(Function<EntityManager, T> action) {
        EntityManager em = createEntityManager();
        try {                      
            em.getTransaction().begin();
            T result = action.apply(em);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public static void shutdown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}