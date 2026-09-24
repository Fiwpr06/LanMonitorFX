package com.fiwpr06.lanmonitorfx;

import com.fiwpr06.lanmonitorfx.ui.ServerMainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

// Điểm khởi chạy JavaFX Application cho máy Giáo viên (Server Dashboard).
public class ServerMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fiwpr06/lanmonitorfx/fxml/server_main.fxml"));
        Parent root = loader.load();

        ServerMainController controller = loader.getController();

        Scene scene = new Scene(root, 1240, 780);
        var cssUrl = getClass().getResource("/com/fiwpr06/lanmonitorfx/css/theme.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        primaryStage.setTitle("LanMonitor — Quản Lý Phòng Máy (Giáo Viên)");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);

        primaryStage.setOnCloseRequest(event -> {
            controller.shutdown();
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();

        // Khởi tạo dịch vụ mạng sau khi giao diện đã sẵn sàng
        controller.initServer();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
