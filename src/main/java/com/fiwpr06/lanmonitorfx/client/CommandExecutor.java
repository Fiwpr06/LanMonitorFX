package com.fiwpr06.lanmonitorfx.client;

import com.fiwpr06.lanmonitorfx.network.Message;
import com.fiwpr06.lanmonitorfx.network.Protocol;
import com.fiwpr06.lanmonitorfx.util.AlertHelper;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Thực thi các lệnh điều khiển hệ thống và giao diện nhận được từ máy Giáo viên.
 * Hỗ trợ các lệnh: LOCK, UNLOCK, SHUTDOWN, RESTART, LOGOUT, NOTIFY.
 * Sử dụng ProcessBuilder để thực thi an toàn các tiến trình Windows (thêm cờ /f force, /t 10).
 * Hỗ trợ callback thông báo để cập nhật vào giao diện AnyDesk Client.
 */
public class CommandExecutor {

    private Stage lockStage;
    private final Consumer<String[]> processRunner;
    private final boolean showUiAlerts;
    private Consumer<String> notificationListener;

    public CommandExecutor() {
        this(args -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(args);
                pb.redirectErrorStream(true);
                pb.start();
            } catch (IOException e) {
                System.err.println("Lỗi thực thi tiến trình hệ thống (" + String.join(" ", args) + "): " + e.getMessage());
            }
        }, true);
    }

    public CommandExecutor(Consumer<String[]> processRunner, boolean showUiAlerts) {
        this.processRunner = processRunner;
        this.showUiAlerts = showUiAlerts;
    }

    public void setNotificationListener(Consumer<String> listener) {
        this.notificationListener = listener;
    }

    /**
     * Phân phối và thực hiện lệnh nhận được từ Server.
     */
    public void execute(Message msg) {
        if (msg == null || msg.command().isBlank()) return;

        switch (msg.command()) {
            case Protocol.CMD_LOCK -> {
                if (showUiAlerts) Platform.runLater(this::showLockScreen);
            }
            case Protocol.CMD_UNLOCK -> {
                if (showUiAlerts) Platform.runLater(this::hideLockScreen);
            }
            case Protocol.CMD_SHUTDOWN -> {
                if (showUiAlerts) {
                    Platform.runLater(() -> AlertHelper.warn(
                            "Lệnh từ Giáo Viên",
                            "⚠️ MÁY TÍNH SẼ TỰ ĐỘNG TẮT NGUỒN TRONG 10 GIÂY!\nVui lòng lưu lại bài làm ngay lập tức."
                    ));
                }
                executeProcess("shutdown.exe", "/s", "/f", "/t", "10", "/c", "May tinh se tat theo lenh Giao vien!");
            }
            case Protocol.CMD_RESTART -> {
                if (showUiAlerts) {
                    Platform.runLater(() -> AlertHelper.warn(
                            "Lệnh từ Giáo Viên",
                            "🔄 MÁY TÍNH SẼ KHỞI ĐỘNG LẠI TRONG 10 GIÂY!\nVui lòng lưu lại bài làm ngay lập tức."
                    ));
                }
                executeProcess("shutdown.exe", "/r", "/f", "/t", "10", "/c", "May tinh se khoi dong lai theo lenh Giao vien!");
            }
            case Protocol.CMD_LOGOUT -> {
                if (showUiAlerts) {
                    Platform.runLater(() -> AlertHelper.warn(
                            "Lệnh từ Giáo Viên",
                            "🚪 PHIÊN ĐĂNG NHẬP ĐANG ĐƯỢC ĐĂNG XUẤT!"
                    ));
                }
                executeProcess("shutdown.exe", "/l", "/f");
            }
            case Protocol.CMD_NOTIFY -> {
                String message = msg.param(0);
                if (showUiAlerts) {
                    // Hiển thị popup nổi luôn trên cùng (AlwaysOnTop) kèm âm thanh
                    AlertHelper.showNotificationPopup("Thông Báo Từ Giáo Viên", message);
                }
                // Đồng thời đẩy tin nhắn vào feed giao diện Client
                if (notificationListener != null) {
                    Platform.runLater(() -> notificationListener.accept(message));
                }
            }
            default -> System.out.println("Lệnh chưa được hỗ trợ hoặc đã bị lược bỏ: " + msg.command());
        }
    }

    // --- LOGIC KHÓA / MỞ KHÓA MÀN HÌNH ---

    private void showLockScreen() {
        if (lockStage == null) {
            lockStage = new Stage(StageStyle.UNDECORATED);
            lockStage.setFullScreen(true);
            lockStage.setFullScreenExitHint("");
            lockStage.setAlwaysOnTop(true);

            // Chặn phím tắt đóng cửa sổ thông thường
            lockStage.setOnCloseRequest(event -> event.consume());

            Label label = new Label("🔒 MÁY TÍNH ĐANG BỊ KHÓA BỞI GIÁO VIÊN");
            label.setTextFill(Color.web("#FF3B56"));
            label.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 38px; -fx-font-weight: bold;");

            StackPane pane = new StackPane(label);
            pane.setAlignment(Pos.CENTER);
            pane.setStyle("-fx-background-color: #0B0E14;");

            Scene scene = new Scene(pane);
            lockStage.setScene(scene);
        }

        if (!lockStage.isShowing()) {
            lockStage.show();
            lockStage.setFullScreen(true);
        }
    }

    private void hideLockScreen() {
        if (lockStage != null && lockStage.isShowing()) {
            lockStage.hide();
        }
    }

    private void executeProcess(String... args) {
        if (processRunner != null) {
            processRunner.accept(args);
        }
    }

    public void cleanup() {
        hideLockScreen();
    }
}
