package ca.seneca.hotel.models;

public enum SentimentTag {
    POSITIVE("Positive"),
    NEUTRAL("Neutral"),
    NEGATIVE("Negative");

    private final String display;

    SentimentTag(String display) {
        this.display = display;
    }

    @Override
    public String toString() {
        return display;
    }

    public static SentimentTag fromRating(int rating) {
        if (rating >= 4) return POSITIVE;
        if (rating == 3) return NEUTRAL;
        return NEGATIVE;
    }
}
