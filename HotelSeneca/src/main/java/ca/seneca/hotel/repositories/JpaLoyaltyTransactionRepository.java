package ca.seneca.hotel.repositories;

import ca.seneca.hotel.models.LoyaltyTransaction;
import ca.seneca.hotel.util.JpaUtil;

import java.util.List;

public class JpaLoyaltyTransactionRepository implements ILoyaltyTransactionRepository {

    @Override
    public LoyaltyTransaction save(LoyaltyTransaction transaction) {
        return JpaUtil.runInTransactionReturning(em -> {
            em.persist(transaction);
            return transaction;
        });
    }

    @Override
    public List<LoyaltyTransaction> findByGuestId(Long guestId) {
        return JpaUtil.runInTransactionReturning(em ->
                em.createQuery(
                                "SELECT t FROM LoyaltyTransaction t WHERE t.guest.id = :id ORDER BY t.createdAt DESC",
                                LoyaltyTransaction.class)
                        .setParameter("id", guestId)
                        .getResultList());
    }
}
