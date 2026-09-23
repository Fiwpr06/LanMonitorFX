module com.fiwpr06.lanmonitorfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.desktop;

    opens com.fiwpr06.lanmonitorfx to javafx.fxml, javafx.graphics;
    opens com.fiwpr06.lanmonitorfx.ui to javafx.fxml;
    opens com.fiwpr06.lanmonitorfx.model to javafx.base;

    exports com.fiwpr06.lanmonitorfx;
    exports com.fiwpr06.lanmonitorfx.server;
    exports com.fiwpr06.lanmonitorfx.client;
    exports com.fiwpr06.lanmonitorfx.network;
    exports com.fiwpr06.lanmonitorfx.model;
    exports com.fiwpr06.lanmonitorfx.ui;
    exports com.fiwpr06.lanmonitorfx.util;
}
