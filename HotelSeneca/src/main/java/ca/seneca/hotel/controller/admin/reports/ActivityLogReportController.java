package ca.seneca.hotel.controller.admin.reports;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.models.ActivityLog;
import ca.seneca.hotel.util.CsvExporter;
import ca.seneca.hotel.util.ExportUtils;
import ca.seneca.hotel.util.LoggerService;
import ca.seneca.hotel.util.TxtExporter;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.Node;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Administrative activity log: who did what, when, filterable by date range, exportable to CSV/TXT. */
public class ActivityLogReportController {

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private TableView<LogRow> logTable;
    @FXML private TableColumn<LogRow, String> colTimestamp;
    @FXML private TableColumn<LogRow, String> colActor;
    @FXML private TableColumn<LogRow, String> colAction;
    @FXML private TableColumn<LogRow, String> colEntityType;
    @FXML private TableColumn<LogRow, Number> colEntityId;
    @FXML private TableColumn<LogRow, String> colMessage;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        colTimestamp.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colActor.setCellValueFactory(new PropertyValueFactory<>("actor"));
        colAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colEntityType.setCellValueFactory(new PropertyValueFactory<>("entityType"));
        colEntityId.setCellValueFactory(new PropertyValueFactory<>("entityId"));
        colMessage.setCellValueFactory(new PropertyValueFactory<>("message"));

        toDate.setValue(LocalDate.now());
        fromDate.setValue(LocalDate.now().minusDays(7));

        onFilter();
    }

    @FXML
    private void onFilter() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        if (from != null && to != null && from.isAfter(to)) {
            statusLabel.setText("Pick a valid date range.");
            return;
        }

        List<ActivityLog> logs = AppContext.activityLogService().findBetween(from, to);
        List<LogRow> rows = new ArrayList<>();
        for (ActivityLog log : logs) {
            rows.add(new LogRow(log.getTimestamp().format(TIMESTAMP_FMT), log.getActor(), log.getAction(),
                    log.getEntityType(), parseEntityId(log.getEntityId()), log.getMessage()));
        }
        logTable.setItems(FXCollections.observableArrayList(rows));
        statusLabel.setText(rows.size() + " entr" + (rows.size() == 1 ? "y" : "ies") + " shown.");
    }

    @FXML
    private void onExportCsv() {
        File file = ExportUtils.chooseSaveFile(logTable, "activity_log.csv", "CSV Files", "*.csv");
        if (file == null) {
            return;
        }
        try {
            CsvExporter.export(headers(), rowsAsStrings(), file);
            statusLabel.setText("Exported to " + file.getName());
        } catch (Exception e) {
            LoggerService.severe("Failed to export the activity log to CSV", e);
            statusLabel.setText("Export failed. See logs for details.");
        }
    }

    @FXML
    private void onExportTxt() {
        File file = ExportUtils.chooseSaveFile(logTable, "activity_log.txt", "Text Files", "*.txt");
        if (file == null) {
            return;
        }
        try {
            TxtExporter.export(headers(), rowsAsStrings(), file);
            statusLabel.setText("Exported to " + file.getName());
        } catch (Exception e) {
            LoggerService.severe("Failed to export the activity log to TXT", e);
            statusLabel.setText("Export failed. See logs for details.");
        }
    }

    private List<String> headers() {
        return List.of("Timestamp", "Actor", "Action", "Entity Type", "Entity ID", "Message");
    }

    private List<List<String>> rowsAsStrings() {
        List<List<String>> out = new ArrayList<>();
        for (LogRow row : logTable.getItems()) {
            out.add(List.of(row.getTimestamp(), row.getActor(), row.getAction(),
                    row.getEntityType(), String.valueOf(row.getEntityId()), row.getMessage()));
        }
        return out;
    }

    /** Entity IDs are stored as free-text (not every entity is numeric), so non-numeric IDs just show as 0. */
    private int parseEntityId(String entityId) {
        try {
            return entityId == null ? 0 : Integer.parseInt(entityId);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static class LogRow {
        private final SimpleStringProperty timestamp, actor, action, entityType, message;
        private final SimpleIntegerProperty entityId;

        public LogRow(String timestamp, String actor, String action,
                      String entityType, int entityId, String message) {
            this.timestamp = new SimpleStringProperty(timestamp);
            this.actor = new SimpleStringProperty(actor);
            this.action = new SimpleStringProperty(action);
            this.entityType = new SimpleStringProperty(entityType);
            this.entityId = new SimpleIntegerProperty(entityId);
            this.message = new SimpleStringProperty(message);
        }

        public String getTimestamp()  { return timestamp.get(); }
        public String getActor()      { return actor.get(); }
        public String getAction()     { return action.get(); }
        public String getEntityType() { return entityType.get(); }
        public int getEntityId()      { return entityId.get(); }
        public String getMessage()    { return message.get(); }
    }
}
