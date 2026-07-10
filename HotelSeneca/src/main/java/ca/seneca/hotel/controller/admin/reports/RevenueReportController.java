package ca.seneca.hotel.controller.admin.reports;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * DEMO controller for Milestone 1 screenshots. Dummy data only.
 * Initialize() will be the ReportingService call from later Milestone 2&3.
 */
public class RevenueReportController {

    @FXML private ComboBox<String> viewCombo;
    @FXML private ComboBox<String> roomTypeCombo;
    @FXML private TableView<RevenueRow> revenueTable;
    @FXML private TableColumn<RevenueRow, String> colPeriod;
    @FXML private TableColumn<RevenueRow, Number> colCount;
    @FXML private TableColumn<RevenueRow, String> colSubtotal;
    @FXML private TableColumn<RevenueRow, String> colTax;
    @FXML private TableColumn<RevenueRow, String> colDiscounts;
    @FXML private TableColumn<RevenueRow, String> colTotal;
    @FXML private Label grandTotalLabel;

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
    }

    //    buttons for later
    @FXML private void onGenerate()  { /* later */ }
    @FXML private void onExportCsv() { /* later */ }
    @FXML private void onExportPdf() { /* later */ }

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
