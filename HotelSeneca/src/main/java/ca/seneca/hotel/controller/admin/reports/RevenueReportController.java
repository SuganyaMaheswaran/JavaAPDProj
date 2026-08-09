package ca.seneca.hotel.controller.admin.reports;

import ca.seneca.hotel.config.AppContext;
import ca.seneca.hotel.models.RoomType;
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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Revenue-by-period report: reservation counts, subtotal/tax/discount/total, filterable by date range and room type. */
public class RevenueReportController {

    @FXML private ComboBox<String> viewCombo;
    @FXML private ComboBox<String> roomTypeCombo;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private TableView<RevenueRow> revenueTable;
    @FXML private TableColumn<RevenueRow, String> colPeriod;
    @FXML private TableColumn<RevenueRow, Number> colCount;
    @FXML private TableColumn<RevenueRow, String> colSubtotal;
    @FXML private TableColumn<RevenueRow, String> colTax;
    @FXML private TableColumn<RevenueRow, String> colDiscounts;
    @FXML private TableColumn<RevenueRow, String> colTotal;
    @FXML private Label grandTotalLabel;

    private final ReportingService reportingService = AppContext.reportingService();

    @FXML
    public void initialize() {
        viewCombo.setItems(FXCollections.observableArrayList("Daily", "Weekly", "Monthly"));
        viewCombo.getSelectionModel().selectFirst();
        roomTypeCombo.setItems(FXCollections.observableArrayList(
                "All", "Single", "Double", "Deluxe", "Penthouse"));
        roomTypeCombo.getSelectionModel().selectFirst();

        colPeriod.setCellValueFactory(new PropertyValueFactory<>("period"));
        colCount.setCellValueFactory(new PropertyValueFactory<>("count"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        colTax.setCellValueFactory(new PropertyValueFactory<>("tax"));
        colDiscounts.setCellValueFactory(new PropertyValueFactory<>("discounts"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        toDate.setValue(LocalDate.now());
        fromDate.setValue(LocalDate.now().minusMonths(1));

        onGenerate();
    }

    @FXML
    private void onGenerate() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        if (from == null || to == null || from.isAfter(to)) {
            grandTotalLabel.setText("Pick a valid date range.");
            return;
        }

        List<ReportingService.RevenueRow> summary = reportingService.revenueSummary(
                from, to, granularityFor(viewCombo.getValue()), roomTypeFor(roomTypeCombo.getValue()));

        double grandTotal = 0;
        List<RevenueRow> tableRows = new ArrayList<>();
        for (ReportingService.RevenueRow row : summary) {
            tableRows.add(new RevenueRow(row.period, row.count,
                    money(row.subtotal), money(row.tax), money(row.discount), money(row.total)));
            grandTotal += row.total;
        }
        revenueTable.setItems(FXCollections.observableArrayList(tableRows));
        grandTotalLabel.setText("Grand Total: " + money(grandTotal));
    }

    @FXML
    private void onExportCsv() {
        File file = ExportUtils.chooseSaveFile(revenueTable, "revenue_report.csv", "CSV Files", "*.csv");
        if (file == null) {
            return;
        }
        try {
            CsvExporter.export(headers(), rowsAsStrings(), file);
            grandTotalLabel.setText("Exported to " + file.getName());
        } catch (Exception e) {
            LoggerService.severe("Failed to export the revenue report to CSV", e);
            grandTotalLabel.setText("Export failed. See logs for details.");
        }
    }

    @FXML
    private void onExportPdf() {
        File file = ExportUtils.chooseSaveFile(revenueTable, "revenue_report.pdf", "PDF Files", "*.pdf");
        if (file == null) {
            return;
        }
        try {
            PdfExporter.export("Revenue Report", headers(), rowsAsStrings(), file);
            grandTotalLabel.setText("Exported to " + file.getName());
        } catch (Exception e) {
            LoggerService.severe("Failed to export the revenue report to PDF", e);
            grandTotalLabel.setText("Export failed. See logs for details.");
        }
    }

    private List<String> headers() {
        return List.of("Period", "Reservations", "Subtotal", "Tax", "Discounts", "Total");
    }

    private List<List<String>> rowsAsStrings() {
        List<List<String>> out = new ArrayList<>();
        for (RevenueRow row : revenueTable.getItems()) {
            out.add(List.of(row.getPeriod(), String.valueOf(row.getCount()),
                    row.getSubtotal(), row.getTax(), row.getDiscounts(), row.getTotal()));
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

    private String money(double value) {
        return String.format("$%.2f", value);
    }

    public static class RevenueRow {
        private final SimpleStringProperty period, subtotal, tax, discounts, total;
        private final SimpleIntegerProperty count;

        public RevenueRow(String period, int count, String subtotal,
                          String tax, String discounts, String total) {
            this.period = new SimpleStringProperty(period);
            this.count = new SimpleIntegerProperty(count);
            this.subtotal = new SimpleStringProperty(subtotal);
            this.tax = new SimpleStringProperty(tax);
            this.discounts = new SimpleStringProperty(discounts);
            this.total = new SimpleStringProperty(total);
        }

        public String getPeriod()    { return period.get(); }
        public int getCount()        { return count.get(); }
        public String getSubtotal()  { return subtotal.get(); }
        public String getTax()       { return tax.get(); }
        public String getDiscounts() { return discounts.get(); }
        public String getTotal()     { return total.get(); }
    }
}
