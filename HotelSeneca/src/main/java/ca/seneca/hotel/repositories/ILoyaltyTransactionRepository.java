package ca.seneca.hotel.repositories;

import ca.seneca.hotel.models.LoyaltyTransaction;

import java.util.List;

public interface ILoyaltyTransactionRepository {
    LoyaltyTransaction save(LoyaltyTransaction transaction);
    List<LoyaltyTransaction> findByGuestId(Long guestId);
}
