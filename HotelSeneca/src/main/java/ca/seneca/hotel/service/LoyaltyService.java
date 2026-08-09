package ca.seneca.hotel.service;

import ca.seneca.hotel.config.LoyaltyPolicy;
import ca.seneca.hotel.models.Guest;
import ca.seneca.hotel.models.LoyaltyTransaction;
import ca.seneca.hotel.models.LoyaltyTxnType;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.repositories.IGuestRepository;
import ca.seneca.hotel.repositories.ILoyaltyTransactionRepository;

import java.time.LocalDateTime;
import java.util.List;

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

    public Guest enroll(Guest guest, String actor) {
        if (guest.getLoyaltyNumber() == null) {
            guest.setLoyaltyNumber("LYLTY-" + String.format("%06d", guest.getId()));
        }
        if (guest.getEnrolledAt() == null) {
            guest.setEnrolledAt(LocalDateTime.now());
        }
        guest.setLoyaltyMember(true);
        Guest saved = guestRepository.save(guest);
        activityLogService.log(actor, "LOYALTY_ENROLL", "Guest", String.valueOf(saved.getId()),
                "Enrolled with loyalty number " + saved.getLoyaltyNumber());
        return saved;
    }

    public void earnPoints(Guest guest, Reservation reservation, double amountPaid, String actor) {
        if (!Boolean.TRUE.equals(guest.getLoyaltyMember())) return;
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

    /** Removes points that were earned from a refunded cash/card payment. */
    public void reverseEarnedPoints(Guest guest, Reservation reservation, double refundedAmount, String actor) {
        int points = LoyaltyPolicy.pointsEarned(refundedAmount);
        if (points <= 0) return;

        guest.setLoyaltyPoints(guest.getLoyaltyPoints() - points);
        guestRepository.save(guest);
        saveRefundReversal(guest, reservation, -points);

        activityLogService.log(actor, "LOYALTY_EARN_REVERSAL", "Guest", String.valueOf(guest.getId()),
                points + " points removed after a $" + String.format("%.2f", refundedAmount) + " refund");
    }

    /** Restores points when a payment originally made with loyalty points is refunded. */
    public void restoreRedeemedPoints(Guest guest, Reservation reservation, double refundedAmount, String actor) {
        int points = (int) Math.ceil(refundedAmount / LoyaltyPolicy.REDEMPTION_RATE);
        if (points <= 0) return;

        guest.setLoyaltyPoints(guest.getLoyaltyPoints() + points);
        guestRepository.save(guest);
        saveRefundReversal(guest, reservation, points);

        activityLogService.log(actor, "LOYALTY_REDEEM_RESTORED", "Guest", String.valueOf(guest.getId()),
                points + " points restored after a $" + String.format("%.2f", refundedAmount) + " refund");
    }

    private void saveRefundReversal(Guest guest, Reservation reservation, int points) {
        LoyaltyTransaction txn = new LoyaltyTransaction();
        txn.setGuest(guest);
        txn.setReservation(reservation);
        txn.setType(LoyaltyTxnType.REFUND_REVERSAL);
        txn.setPoints(points);
        loyaltyTransactionRepository.save(txn);
    }

    public List<LoyaltyTransaction> getHistory(Guest guest) {
        return loyaltyTransactionRepository.findByGuestId(guest.getId());
    }
}
