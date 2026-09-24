package com.fiwpr06.lanmonitorfx.util;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.Toolkit;
import java.util.Optional;

// Tiện ích hiển thị các hộp thoại thông báo, cảnh báo, thông báo nổi AnyDesk đồng bộ theo theme dự án.
public final class AlertHelper {

    private static final String THEME_CSS = "/com/fiwpr06/lanmonitorfx/css/theme.css";

    private AlertHelper() {}

    private static void applyTheme(DialogPane dialogPane) {
        try {
            var cssUrl = AlertHelper.class.getResource(THEME_CSS);
            if (cssUrl != null) {
                dialogPane.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception ignored) {}
    }

    // Hiển thị thông báo thông tin (Information).
    public static void info(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        applyTheme(alert.getDialogPane());
        alert.showAndWait();
    }

    // Hiển thị thông báo cảnh báo (Warning).
    public static void warn(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        applyTheme(alert.getDialogPane());
        alert.showAndWait();
    }

    /**
     * Hiển thị hộp thoại xác nhận Có / Không (Confirmation).
     *
     * @return true nếu người dùng chọn OK, false nếu Cancel hoặc đóng dialog
     */
    public static boolean confirm(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        applyTheme(alert.getDialogPane());

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Hiển thị hộp thoại nhập văn bản (TextInputDialog).
     */
    public static Optional<String> textInput(String title, String prompt) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(prompt);
        applyTheme(dialog.getDialogPane());
        return dialog.showAndWait();
    }

    /**
     * Cửa sổ thông báo nổi chuyên dụng đè lên tất cả ứng dụng (Always On Top) kèm chuông cảnh báo.
     * Đảm bảo sinh viên đang làm bài hoặc mở bất kỳ app nào cũng lập tức nhìn thấy thông báo từ Giáo viên.
     * Tự động phân biệt và tô màu:
     * - Tin riêng: Viền vàng hổ phách, icon 🔒, tiêu đề "TIN NHẮN RIÊNG TỪ GIÁO VIÊN"
     * - Tin cả lớp: Viền xanh Cyan, icon 📢, tiêu đề "THÔNG BÁO CHUNG CHO CẢ LỚP"
     */
    public static void showNotificationPopup(String title, String message) {
        Platform.runLater(() -> {
            try {
                // Phát âm thanh cảnh báo hệ thống
                Toolkit.getDefaultToolkit().beep();

                boolean isPrivate = message.startsWith("[GỬI RIÊNG]")
                        || message.contains("[TIN NHẮN RIÊNG]")
                        || message.contains("[GỬI RIÊNG NHÓM")
                        || title.contains("RIÊNG");

                String borderColor = isPrivate ? "#F59E0B" : "#00D2E6";
                String titleColor = isPrivate ? "#F59E0B" : "#00D2E6";
                String icon = isPrivate ? "🔒" : "📢";
                String scopeTag = isPrivate ? "TIN NHẮN RIÊNG TỪ GIÁO VIÊN" : "THÔNG BÁO CHUNG CHO CẢ LỚP";
                String scopeSub = isPrivate ? "(Chỉ gửi riêng cho máy tính của bạn, các bạn khác không nhận được)" : "(Thông báo phát sóng chung tới toàn thể phòng máy)";

                Stage stage = new Stage(StageStyle.UTILITY);
                stage.setTitle(scopeTag);
                stage.initModality(Modality.NONE);
                stage.setAlwaysOnTop(true);

                VBox root = new VBox(12);
                root.setPadding(new Insets(18));
                root.setStyle("-fx-background-color: #0F141C; -fx-border-color: " + borderColor + "; -fx-border-width: 2px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-effect: dropshadow(gaussian, " + (isPrivate ? "rgba(245, 158, 11, 0.35)" : "rgba(0, 210, 230, 0.35)") + ", 12, 0, 0, 2);");

                HBox header = new HBox(10);
                header.setAlignment(Pos.CENTER_LEFT);
                Label iconLbl = new Label(icon);
                iconLbl.setStyle("-fx-font-size: 26px;");

                VBox headerTexts = new VBox(2);
                Label titleLbl = new Label(scopeTag);
                titleLbl.setStyle("-fx-font-family: 'Segoe UI', Arial; -fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + titleColor + ";");
                Label subLbl = new Label(scopeSub);
                subLbl.setStyle("-fx-font-family: 'Segoe UI', Arial; -fx-font-size: 11px; -fx-text-fill: #94A3B8;");
                headerTexts.getChildren().addAll(titleLbl, subLbl);

                header.getChildren().addAll(iconLbl, headerTexts);

                TextArea msgArea = new TextArea(message);
                msgArea.setWrapText(true);
                msgArea.setEditable(false);
                msgArea.setPrefRowCount(4);
                msgArea.setPrefWidth(450);
                msgArea.setStyle("-fx-control-inner-background: #080B10; -fx-background-color: #080B10; -fx-text-fill: #F0F4F8; -fx-font-size: 14px; -fx-font-family: 'Segoe UI', Arial;");

                Button btnClose = new Button("Đã hiểu (Đóng)");
                btnClose.setDefaultButton(true);
                btnClose.setStyle("-fx-background-color: " + borderColor + "; -fx-text-fill: #0B0E14; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
                btnClose.setOnAction(e -> stage.close());

                HBox btnBox = new HBox(btnClose);
                btnBox.setAlignment(Pos.CENTER_RIGHT);

                root.getChildren().addAll(header, msgArea, btnBox);

                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setResizable(false);
                stage.toFront();
                stage.show();
            } catch (Exception e) {
                System.err.println("Lỗi hiển thị thông báo nổi: " + e.getMessage());
            }
        });
    }

    /**
     * Hiển thị cửa sổ xem nội dung nhật ký với TextArea cuộn được, chỉ đọc.
     */
    public static void showLogViewer(String title, String logContent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("Nhật Ký Hoạt Động Phòng Máy");

        TextArea textArea = new TextArea(logContent);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(650);
        textArea.setPrefHeight(450);
        textArea.setStyle("-fx-font-family: 'Segoe UI', 'Consolas', monospace; -fx-text-fill: #38BDF8; -fx-control-inner-background: #080B10; -fx-background-color: #080B10;");

        alert.getDialogPane().setContent(textArea);
        applyTheme(alert.getDialogPane());
        alert.showAndWait();
    }
}
