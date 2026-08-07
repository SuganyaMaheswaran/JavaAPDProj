package ca.seneca.hotel.service;

import ca.seneca.hotel.models.Feedback;
import ca.seneca.hotel.models.Reservation;
import ca.seneca.hotel.models.ReservationStatus;
import ca.seneca.hotel.models.SentimentTag;
import ca.seneca.hotel.repositories.IFeedbackRepository;

import java.util.List;

/**
 * Checkout already refuses to complete while a balance remains (see
 * {@code CheckoutController}), so a CHECKED_OUT reservation is guaranteed settled --
 * eligibility here only needs to check status and one-feedback-per-stay.
 */
public class FeedbackService {

    private final IFeedbackRepository feedbackRepository;
    private final ActivityLogService activityLogService;

    public FeedbackService(IFeedbackRepository feedbackRepository, ActivityLogService activityLogService) {
        this.feedbackRepository = feedbackRepository;
        this.activityLogService = activityLogService;
    }

    public void checkEligible(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            throw new IllegalStateException("Feedback can only be submitted after checkout and a settled balance.");
        }
        if (feedbackRepository.existsByReservationId(reservation.getId())) {
            throw new IllegalStateException("Feedback has already been submitted for this reservation.");
        }
    }

    public Feedback submit(Reservation reservation, int rating, String comment) {
        checkEligible(reservation);
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        Feedback feedback = new Feedback();
        feedback.setReservation(reservation);
        feedback.setGuest(reservation.getGuest());
        feedback.setRating(rating);
        feedback.setComment(comment != null && comment.length() > Feedback.MAX_COMMENT_LENGTH
                ? comment.substring(0, Feedback.MAX_COMMENT_LENGTH) : comment);
        feedback.setSentimentTag(SentimentTag.fromRating(rating));

        Feedback saved = feedbackRepository.save(feedback);
        activityLogService.log(reservation.getGuest().getEmail(), "FEEDBACK_SUBMIT", "Reservation",
                String.valueOf(reservation.getId()), rating + "-star feedback submitted");
        return saved;
    }

    public List<Feedback> findAll() {
        return feedbackRepository.findAll();
    }
}
