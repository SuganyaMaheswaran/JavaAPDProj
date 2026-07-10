/**
 */
module com.hotel {
    requires javafx.controls;
    requires javafx.fxml;

    opens ca.seneca.hotel.controller to javafx.fxml, javafx.base;
    exports ca.seneca.hotel.app;
    opens ca.seneca.hotel.controller.admin.reports to javafx.base, javafx.fxml;
    opens ca.seneca.hotel.controller.kiosk to javafx.base, javafx.fxml;
    opens ca.seneca.hotel.controller.admin to javafx.base, javafx.fxml;
}
