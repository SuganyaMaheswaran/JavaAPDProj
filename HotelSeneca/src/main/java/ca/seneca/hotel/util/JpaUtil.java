package ca.seneca.hotel.util;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Singleton EntityManagerFactory for the whole application.
 * Provides centralized management for JPA operations.
 */
public class JpaUtil {
    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("HotelPU");

    private JpaUtil() {} // Prevent instantiation

    public static EntityManager createEntityManager() {
        return emf.createEntityManager();
    }

    /**
     * Execution helper without result returning
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
     * Execution helper with result returning
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