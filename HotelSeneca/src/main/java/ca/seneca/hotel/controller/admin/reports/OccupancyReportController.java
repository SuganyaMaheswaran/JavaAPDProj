package ca.seneca.hotel.controller.admin.reports;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.models.RoomType;
import ca.seneca.hotel.security.CurrentSession;
import ca.seneca.hotel.service.ReportingService;
import ca.seneca.hotel.util.CsvExporter;
import ca.seneca.hotel.util.ExportUtils;
import ca.seneca.hotel.util.LoggerService;
import ca.seneca.hotel.util.PdfExporter;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Occupancy-by-period report: rooms available/occupied and occupancy %, filterable by date range and room type. */
public class OccupancyReportController {

    private static final int MAX_RANGE_DAYS = 366;

    @FXML private ComboBox<String> viewCombo;
    @FXML private ComboBox<String> roomTypeCombo;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private TableView<OccupancyRow> occupancyTable;
    @FXML private TableColumn<OccupancyRow, String> colDate;
    @FXML private TableColumn<OccupancyRow, Number> colAvailable;
    @FXML private TableColumn<OccupancyRow, Number> colOccupied;
    @FXML private TableColumn<OccupancyRow, String> colPercent;
    @FXML private Label statusLabel;

    private final ReportingService reportingService = AppContext.reportingService();
    private boolean readyToLog;

    @FXML
    public void initialize() {
        viewCombo.setItems(FXCollections.observableArrayList("Daily", "Weekly", "Monthly"));
        viewCombo.getSelectionModel().selectFirst();
        roomTypeCombo.setItems(FXCollections.observableArrayList(
                "All", "Single", "Double", "Deluxe", "Penthouse"));
        roomTypeCombo.getSelectionModel().selectFirst();

        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colAvailable.setCellValueFactory(new PropertyValueFactory<>("available"));
        colOccupied.setCellValueFactory(new PropertyValueFactory<>("occupied"));
        colPercent.setCellValueFactory(new PropertyValueFactory<>("percent"));

        toDate.setValue(LocalDate.now());
        fromDate.setValue(LocalDate.now().minusDays(30));

        onGenerate();
        readyToLog = true;
    }

    @FXML
    private void onGenerate() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        if (from == null || to == null || from.isAfter(to)) {
            statusLabel.setText("Pick a valid date range.");
            return;
        }
        if (from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            statusLabel.setText("Date range is too large -- please narrow it to a year or less.");
            return;
        }

        List<ReportingService.OccupancyRow> summary = reportingService.occupancyReport(
                from, to, granularityFor(viewCombo.getValue()), roomTypeFor(roomTypeCombo.getValue()));

        List<OccupancyRow> tableRows = new ArrayList<>();
        for (ReportingService.OccupancyRow row : summary) {
            tableRows.add(new OccupancyRow(row.period, row.available, row.occupied,
                    String.format("%.1f", row.percent())));
        }
        occupancyTable.setItems(FXCollections.observableArrayList(tableRows));
        statusLabel.setText(tableRows.size() + " period(s) shown.");
        if (readyToLog) {
            AppContext.activityLogService().log(CurrentSession.actorName(), "REPORT_GENERATE", "Report", "OCCUPANCY",
                    "Occupancy report: " + from + " to " + to + ", view=" + viewCombo.getValue()
                            + ", roomType=" + roomTypeCombo.getValue() + ", rows=" + tableRows.size());
        }
    }

    @FXML
    private void onExportCsv() {
        File file = ExportUtils.chooseSaveFile(occupancyTable, "occupancy_report.csv", "CSV Files", "*.csv");
        if (file == null) {
            return;
        }
        try {
            CsvExporter.export(headers(), rowsAsStrings(), file);
            statusLabel.setText("Exported to " + file.getName());
        } catch (Exception e) {
            LoggerService.severe("Failed to export the occupancy report to CSV", e);
            statusLabel.setText("Export failed. See logs for details.");
        }
    }

    @FXML
    private void onExportPdf() {
        File file = ExportUtils.chooseSaveFile(occupancyTable, "occupancy_report.pdf", "PDF Files", "*.pdf");
        if (file == null) {
            return;
        }
        try {
            PdfExporter.export("Occupancy Report", headers(), rowsAsStrings(), file);
            statusLabel.setText("Exported to " + file.getName());
        } catch (Exception e) {
            LoggerService.severe("Failed to export the occupancy report to PDF", e);
            statusLabel.setText("Export failed. See logs for details.");
        }
    }

    private List<String> headers() {
        return List.of("Date", "Rooms Available", "Rooms Occupied", "Occupancy %");
    }

    private List<List<String>> rowsAsStrings() {
        List<List<String>> out = new ArrayList<>();
        for (OccupancyRow row : occupancyTable.getItems()) {
            out.add(List.of(row.getDate(), String.valueOf(row.getAvailable()),
                    String.valueOf(row.getOccupied()), row.getPercent()));
        }
        return out;
    }


    private ReportingService.Granularity granularityFor(String label) {
        if ("Weekly".equals(label)) return ReportingService.Granularity.WEEK;
        if ("Monthly".equals(label)) return ReportingService.Granularity.MONTH;
        return ReportingService.Granularity.DAY;
    }

    private RoomType roomTypeFor(String label) {
        return (label == null || "All".equals(label)) ? null : RoomType.valueOf(label.toUpperCase());
    }

    public static class OccupancyRow {
        private final SimpleStringProperty date, percent;
        private final SimpleIntegerProperty available, occupied;

        public OccupancyRow(String date, int available, int occupied, String percent) {
            this.date = new SimpleStringProperty(date);
            this.available = new SimpleIntegerProperty(available);
            this.occupied = new SimpleIntegerProperty(occupied);
            this.percent = new SimpleStringProperty(percent);
        }

        public String getDate()   { return date.get(); }
        public int getAvailable() { return available.get(); }
        public int getOccupied()  { return occupied.get(); }
        public String getPercent(){ return percent.get(); }
    }
}
