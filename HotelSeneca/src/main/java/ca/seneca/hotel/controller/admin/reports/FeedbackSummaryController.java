package ca.seneca.hotel.controller.admin.reports;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.models.Feedback;
import ca.seneca.hotel.service.FeedbackService;
import ca.seneca.hotel.util.CsvExporter;
import ca.seneca.hotel.util.ExportUtils;
import ca.seneca.hotel.util.LoggerService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class FeedbackSummaryController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private TextField guestField;
    @FXML private ComboBox<String> ratingCombo;
    @FXML private ComboBox<String> sentimentCombo;
    @FXML private TableView<FeedbackRow> feedbackTable;
    @FXML private TableColumn<FeedbackRow, Number> colReservationId;
    @FXML private TableColumn<FeedbackRow, String> colGuest;
    @FXML private TableColumn<FeedbackRow, Number> colRating;
    @FXML private TableColumn<FeedbackRow, String> colComment;
    @FXML private TableColumn<FeedbackRow, String> colDate;
    @FXML private TableColumn<FeedbackRow, String> colSentiment;
    @FXML private Label avgRatingLabel;
    @FXML private Label tagCountsLabel;

    private final FeedbackService feedbackService = AppContext.feedbackService();
    private List<Feedback> feedbackEntries = new ArrayList<>();

    @FXML
    public void initialize() {
        ratingCombo.setItems(FXCollections.observableArrayList("All", "5", "4", "3", "2", "1"));
        ratingCombo.getSelectionModel().selectFirst();
        sentimentCombo.setItems(FXCollections.observableArrayList(
                "All", "Positive", "Neutral", "Negative"));
        sentimentCombo.getSelectionModel().selectFirst();

        colReservationId.setCellValueFactory(new PropertyValueFactory<>("reservationId"));
        colGuest.setCellValueFactory(new PropertyValueFactory<>("guest"));
        colRating.setCellValueFactory(new PropertyValueFactory<>("rating"));
        colComment.setCellValueFactory(new PropertyValueFactory<>("comment"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colSentiment.setCellValueFactory(new PropertyValueFactory<>("sentiment"));

        loadFeedback();
    }

    private void loadFeedback() {
        try {
            feedbackEntries = feedbackService.findAll();
            displayFeedback(feedbackEntries);
        } catch (RuntimeException exception) {
            LoggerService.severe("Unable to load the feedback summary", exception);
            feedbackTable.getItems().clear();
            updateSummary(List.of());
            showError("Unable to load feedback from the database.");
        }
    }

    @FXML
    private void applyFilters() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();

        if (from != null && to != null && from.isAfter(to)) {
            showError("The From date cannot be after the To date.");
            return;
        }

        String guestQuery = guestField.getText() == null
                ? "" : guestField.getText().trim().toLowerCase(Locale.ROOT);
        String selectedRating = ratingCombo.getValue();
        String selectedSentiment = sentimentCombo.getValue();

        List<Feedback> filtered = feedbackEntries.stream()
                .filter(feedback -> matchesGuest(feedback, guestQuery))
                .filter(feedback -> matchesRating(feedback, selectedRating))
                .filter(feedback -> matchesSentiment(feedback, selectedSentiment))
                .filter(feedback -> matchesDates(feedback, from, to))
                .collect(Collectors.toList());

        displayFeedback(filtered);
    }

<<<<<<< HEAD
    @FXML
    private void onExportCsv() {
        File file = ExportUtils.chooseSaveFile(feedbackTable, "feedback_summary.csv", "CSV Files", "*.csv");
        if (file == null) {
            return;
        }
        try {
            List<String> headers = List.of("Reservation Number", "Guest", "Rating", "Comment", "Date", "Sentiment Tag");
            List<List<String>> rows = new ArrayList<>();
            for (FeedbackRow row : feedbackTable.getItems()) {
                rows.add(List.of(String.valueOf(row.getReservationId()), row.getGuest(),
                        String.valueOf(row.getRating()), row.getComment(), row.getDate(), row.getSentiment()));
            }
            CsvExporter.export(headers, rows, file);
        } catch (Exception e) {
            LoggerService.severe("Failed to export the feedback summary to CSV", e);
            showError("Unable to export the feedback summary. See logs for details.");
        }
    }

=======
>>>>>>> origin/main
    private boolean matchesGuest(Feedback feedback, String query) {
        if (query.isEmpty()) return true;
        String fullName = feedback.getGuest().getFirstName() + " " + feedback.getGuest().getLastName();
        return fullName.toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean matchesRating(Feedback feedback, String rating) {
        return rating == null || rating.equals("All")
                || feedback.getRating() == Integer.parseInt(rating);
    }

    private boolean matchesSentiment(Feedback feedback, String sentiment) {
        return sentiment == null || sentiment.equals("All")
                || feedback.getSentimentTag().toString().equalsIgnoreCase(sentiment);
    }

    private boolean matchesDates(Feedback feedback, LocalDate from, LocalDate to) {
        LocalDate date = feedback.getCreatedAt().toLocalDate();
        return (from == null || !date.isBefore(from))
                && (to == null || !date.isAfter(to));
    }

    private void displayFeedback(List<Feedback> entries) {
        List<FeedbackRow> rows = entries.stream()
                .map(this::toRow)
                .collect(Collectors.toList());
        feedbackTable.setItems(FXCollections.observableArrayList(rows));
        updateSummary(entries);
    }

    private FeedbackRow toRow(Feedback feedback) {
        String guest = feedback.getGuest().getFirstName() + " " + feedback.getGuest().getLastName();
        String date = feedback.getCreatedAt().format(DATE_FORMAT);
        return new FeedbackRow(
                feedback.getReservation().getId(),
                guest,
                feedback.getRating(),
                feedback.getComment(),
                date,
                feedback.getSentimentTag().toString());
    }

    private void updateSummary(List<Feedback> entries) {
        double average = entries.stream().mapToInt(Feedback::getRating).average().orElse(0);
        long cleanliness = entries.stream().filter(f -> f.getCleanlinessRating() <= 2).count();
        long service = entries.stream().filter(f -> f.getServiceRating() <= 2).count();
        long comfort = entries.stream().filter(f -> f.getComfortRating() <= 2).count();
        long value = entries.stream().filter(f -> f.getValueRating() <= 2).count();

        avgRatingLabel.setText(String.format("Average Rating: %.1f / 5", average));
        tagCountsLabel.setText(String.format(
                "Issue tags: Cleanliness %d | Service %d | Comfort %d | Value %d",
                cleanliness, service, comfort, value));
    }

<<<<<<< HEAD
=======
    @FXML
    private void onExportCsv() {
        if (feedbackTable.getItems().isEmpty()) {
            showError("There is nothing to export. Adjust the filters and try again.");
            return;
        }

        File file = chooseFile("feedback_summary.csv", "CSV Files", "*.csv");
        if (file == null) {
            return;
        }
        try {
            CsvExporter.export(headers(), rowsAsStrings(), file);
            tagCountsLabel.setText("Exported to " + file.getName());
        } catch (Exception e) {
            LoggerService.severe("Failed to export the feedback summary to CSV", e);
            tagCountsLabel.setText("Export failed. See logs for details.");
        }
    }

    private List<String> headers() {
        return List.of("Reservation Number", "Guest", "Rating", "Comment", "Date", "Sentiment Tag");
    }

    private List<List<String>> rowsAsStrings() {
        List<List<String>> out = new ArrayList<>();
        for (FeedbackRow row : feedbackTable.getItems()) {
            out.add(List.of(
                    String.valueOf(row.getReservationId()),
                    row.getGuest(),
                    String.valueOf(row.getRating()),
                    row.getComment(),
                    row.getDate(),
                    row.getSentiment()));
        }
        return out;
    }

    private File chooseFile(String suggestedName, String description, String extensionFilter) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(suggestedName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extensionFilter));
        return chooser.showSaveDialog(feedbackTable.getScene().getWindow());
    }

>>>>>>> origin/main
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Feedback Summary");
        alert.setHeaderText("Unable to complete the request");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class FeedbackRow {
        private final SimpleLongProperty reservationId;
        private final SimpleIntegerProperty rating;
        private final SimpleStringProperty guest, comment, date, sentiment;

        public FeedbackRow(long reservationId, String guest, int rating,
                           String comment, String date, String sentiment) {
            this.reservationId = new SimpleLongProperty(reservationId);
            this.guest = new SimpleStringProperty(guest);
            this.rating = new SimpleIntegerProperty(rating);
            this.comment = new SimpleStringProperty(comment == null ? "" : comment);
            this.date = new SimpleStringProperty(date);
            this.sentiment = new SimpleStringProperty(sentiment);
        }

        public long getReservationId() { return reservationId.get(); }
        public String getGuest() { return guest.get(); }
        public int getRating() { return rating.get(); }
        public String getComment() { return comment.get(); }
        public String getDate() { return date.get(); }
        public String getSentiment() { return sentiment.get(); }
    }
}
