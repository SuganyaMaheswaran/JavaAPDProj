package ca.seneca.hotel.service;

import ca.seneca.hotel.config.LoyaltyPolicy;
import ca.seneca.hotel.models.Guest;
import ca.seneca.hotel.models.LoyaltyTransaction;
import ca.seneca.hotel.models.LoyaltyTxnType;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.repositories.IGuestRepository;
import ca.seneca.hotel.repositories.ILoyaltyTransactionRepository;

import java.util.List;

/** Owns the real loyalty ledger: enrollment/number issuance, earning and redemption. */
public class LoyaltyService {

    private final IGuestRepository guestRepository;
    private final ILoyaltyTransactionRepository loyaltyTransactionRepository;
    private final ActivityLogService activityLogService;

    public LoyaltyService(IGuestRepository guestRepository,
                          ILoyaltyTransactionRepository loyaltyTransactionRepository,
                          ActivityLogService activityLogService) {
        this.guestRepository = guestRepository;
        this.loyaltyTransactionRepository = loyaltyTransactionRepository;
        this.activityLogService = activityLogService;
    }

    /** Enrolls an already-persisted guest and issues a loyalty number. */
    public Guest enroll(Guest guest, String actor) {
        if (guest.getLoyaltyNumber() == null) {
            guest.setLoyaltyNumber("LYLTY-" + String.format("%06d", guest.getId()));
        }
        guest.setLoyaltyMember(true);
        Guest saved = guestRepository.save(guest);
        activityLogService.log(actor, "LOYALTY_ENROLL", "Guest", String.valueOf(saved.getId()),
                "Enrolled with loyalty number " + saved.getLoyaltyNumber());
        return saved;
    }

    public void earnPoints(Guest guest, Reservation reservation, double amountPaid, String actor) {
        int points = LoyaltyPolicy.pointsEarned(amountPaid);
        if (points <= 0) return;

        guest.setLoyaltyPoints(guest.getLoyaltyPoints() + points);
        guestRepository.save(guest);

        LoyaltyTransaction txn = new LoyaltyTransaction();
        txn.setGuest(guest);
        txn.setReservation(reservation);
        txn.setType(LoyaltyTxnType.EARN);
        txn.setPoints(points);
        loyaltyTransactionRepository.save(txn);

        activityLogService.log(actor, "LOYALTY_EARN", "Guest", String.valueOf(guest.getId()),
                points + " points earned from a $" + String.format("%.2f", amountPaid) + " payment");
    }

    /** Deducts the already-capped point count a {@code LoyaltyRedemptionStrategy} decided to apply. */
    public void redeemPoints(Guest guest, Reservation reservation, int points, String actor) {
        if (points <= 0) return;
        int applied = Math.min(points, guest.getLoyaltyPoints());
        guest.setLoyaltyPoints(guest.getLoyaltyPoints() - applied);
        guestRepository.save(guest);

        LoyaltyTransaction txn = new LoyaltyTransaction();
        txn.setGuest(guest);
        txn.setReservation(reservation);
        txn.setType(LoyaltyTxnType.REDEEM);
        txn.setPoints(applied);
        loyaltyTransactionRepository.save(txn);

        activityLogService.log(actor, "LOYALTY_REDEEM", "Guest", String.valueOf(guest.getId()),
                applied + " points redeemed");
    }

    public List<LoyaltyTransaction> getHistory(Guest guest) {
        return loyaltyTransactionRepository.findByGuestId(guest.getId());
    }
}
