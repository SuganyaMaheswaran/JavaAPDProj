package ca.seneca.hotel.controller.admin.reports;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/** DEMO controller for Milestone 1 screenshots. Dummy data only. */
public class OccupancyReportController {

    @FXML private ComboBox<String> viewCombo;
    @FXML private ComboBox<String> roomTypeCombo;
    @FXML private TableView<OccupancyRow> occupancyTable;
    @FXML private TableColumn<OccupancyRow, String> colDate;
    @FXML private TableColumn<OccupancyRow, Number> colAvailable;
    @FXML private TableColumn<OccupancyRow, Number> colOccupied;
    @FXML private TableColumn<OccupancyRow, String> colPercent;

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
