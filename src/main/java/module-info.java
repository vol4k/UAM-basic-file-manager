module com.s474137.tcmd {
    requires transitive javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;

    opens com.s474137.tcmd to javafx.fxml;
    exports com.s474137.tcmd;
}
