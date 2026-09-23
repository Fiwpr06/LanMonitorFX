package com.fiwpr06.lanmonitorfx;

import com.fiwpr06.lanmonitorfx.ui.ClientMainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

// Điểm khởi chạy JavaFX Application cho máy Sinh viên (Client).
public class ClientMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fiwpr06/lanmonitorfx/fxml/client_main.fxml"));
        Parent root = loader.load();

        ClientMainController controller = loader.getController();

        Scene scene = new Scene(root, 790, 550);
        var cssUrl = getClass().getResource("/com/fiwpr06/lanmonitorfx/css/theme.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        primaryStage.setTitle("LanMonitor — Bàn Làm Việc Sinh Viên");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(720);
        primaryStage.setMinHeight(500);

        primaryStage.setOnCloseRequest(event -> {
            controller.shutdown();
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
