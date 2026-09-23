package com.fiwpr06.lanmonitorfx.ui;

import com.fiwpr06.lanmonitorfx.model.ClientSession;
import com.fiwpr06.lanmonitorfx.network.Protocol;
import com.fiwpr06.lanmonitorfx.util.FileLogger;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Điều khiển một ô thẻ hiển thị ảnh chụp màn hình thu nhỏ (thumbnail) của sinh viên trong lưới giám sát.
 * Tự động cập nhật viền, huy hiệu trạng thái (Online / Offline / Cảnh báo) và độ trong suốt của thumbnail.
 * Hỗ trợ nhấp chuột để phóng to, sao chép IP và menu ngữ cảnh điều khiển nhanh.
 */
public class ClientCardController {

    @FXML private VBox cardRoot;
    @FXML private Label statusBadge;
    @FXML private Label ipLabel;
    @FXML private ImageView screenImage;
    @FXML private Label nameLabel;

    private ClientSession session;
    private ServerMainController mainController;

    public void init(ClientSession session) {
        init(session, null);
    }

    // Khởi tạo liên kết dữ liệu giữa thẻ và phiên ClientSession.
    public void init(ClientSession session, ServerMainController mainController) {
        this.session = session;
        this.mainController = mainController;

        // Hiển thị IP trên góc phải
        ipLabel.textProperty().bind(session.ipAddressProperty());

        // Hiển thị Tên và MSSV
        nameLabel.textProperty().bind(Bindings.createStringBinding(
                () -> session.getHoTen() + " [" + session.getMssv() + "]",
                session.hoTenProperty(),
                session.mssvProperty()
        ));

        // Tự động cập nhật ảnh khi ClientSession nhận được ảnh mới
        screenImage.imageProperty().bind(session.lastScreenProperty());

        // Lắng nghe thay đổi trạng thái và cảnh báo để cập nhật giao diện
        session.statusProperty().addListener((obs, oldVal, newVal) -> Platform.runLater(this::updateVisualState));
        session.warningProperty().addListener((obs, oldVal, newVal) -> Platform.runLater(this::updateVisualState));

        // Cập nhật trạng thái hiển thị ban đầu
        updateVisualState();

        // Click vào nhãn IP để sao chép nhanh
        ipLabel.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                event.consume();
                ServerMainController.copyToClipboard(session.getIpAddress());
                if (mainController != null) {
                    mainController.setStatus("📋 Đã sao chép IP (" + session.getIpAddress() + ") của sinh viên " + session.getHoTen());
                }
            }
        });

        // Thiết lập menu ngữ cảnh (Right-click) cho thẻ
        setupCardContextMenu();

        // Nhấn đúp hoặc nhấp chuột trái vào thẻ để mở cửa sổ phóng to
        cardRoot.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                openZoomWindow();
            }
        });
    }

    private void setupCardContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem copyIpItem = new MenuItem("📋 Sao chép IP");
        copyIpItem.setOnAction(e -> {
            ServerMainController.copyToClipboard(session.getIpAddress());
            if (mainController != null) {
                mainController.setStatus("📋 Đã sao chép IP (" + session.getIpAddress() + ") của sinh viên " + session.getHoTen());
            }
        });

        MenuItem sendNotifyItem = new MenuItem("💬 Gửi thông báo riêng");
        sendNotifyItem.setOnAction(e -> {
            if (mainController != null) {
                mainController.sendNotifyToClient(session);
            }
        });

        MenuItem lockItem = new MenuItem("🔒 Khóa máy này");
        lockItem.setOnAction(e -> {
            if (mainController != null && mainController.getRegistry() != null) {
                mainController.getRegistry().sendTo(session.getClientId(), Protocol.CMD_LOCK);
                session.setWarning(true);
                FileLogger.log("🔒 Giáo viên đã phát lệnh KHÓA máy sinh viên: " + session.getHoTen());
            }
        });

        MenuItem unlockItem = new MenuItem("🔓 Mở khóa máy này");
        unlockItem.setOnAction(e -> {
            if (mainController != null && mainController.getRegistry() != null) {
                mainController.getRegistry().sendTo(session.getClientId(), Protocol.CMD_UNLOCK);
                session.setWarning(false);
                FileLogger.log("🔓 Giáo viên đã phát lệnh MỞ KHÓA máy sinh viên: " + session.getHoTen());
            }
        });

        MenuItem shutdownItem = new MenuItem("⚡ Tắt nguồn máy này");
        shutdownItem.setOnAction(e -> {
            if (mainController != null) {
                mainController.shutdownSingleClient(session);
            }
        });

        MenuItem restartItem = new MenuItem("🔄 Khởi động lại máy này");
        restartItem.setOnAction(e -> {
            if (mainController != null) {
                mainController.restartSingleClient(session);
            }
        });

        MenuItem zoomItem = new MenuItem("👀 Phóng to màn hình");
        zoomItem.setOnAction(e -> openZoomWindow());

        contextMenu.getItems().addAll(
                zoomItem,
                copyIpItem,
                new SeparatorMenuItem(),
                sendNotifyItem,
                lockItem,
                unlockItem,
                new SeparatorMenuItem(),
                shutdownItem,
                restartItem
        );

        cardRoot.setOnContextMenuRequested(event -> {
            contextMenu.show(cardRoot, event.getScreenX(), event.getScreenY());
            event.consume();
        });
    }

    private void updateVisualState() {
        boolean isOnline = "Online".equalsIgnoreCase(session.getStatus());
        boolean isWarning = session.isWarning();

        cardRoot.getStyleClass().removeAll("card-online", "card-offline", "card-warning");

        if (isWarning) {
            cardRoot.getStyleClass().add("card-warning");
            statusBadge.setText("● Cảnh báo");
            statusBadge.setStyle("-fx-text-fill: #FF3B56; -fx-font-weight: bold;");
            screenImage.setOpacity(1.0);
        } else if (isOnline) {
            cardRoot.getStyleClass().add("card-online");
            statusBadge.setText("● Online");
            statusBadge.setStyle("-fx-text-fill: #2EE68A; -fx-font-weight: bold;");
            screenImage.setOpacity(1.0);
        } else {
            cardRoot.getStyleClass().add("card-offline");
            statusBadge.setText("○ Offline");
            statusBadge.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold;");
            screenImage.setOpacity(0.4);
        }
    }

    // Mở cửa sổ riêng biệt để xem màn hình sinh viên ở kích thước lớn.
    private void openZoomWindow() {
        Stage zoomStage = new Stage();
        zoomStage.setTitle("Giám sát màn hình: " + session.getHoTen() + " [" + session.getMssv() + "] - " + session.getIpAddress());

        ImageView zoomedView = new ImageView();
        zoomedView.setPreserveRatio(true);
        zoomedView.setSmooth(true);
        zoomedView.imageProperty().bind(session.lastScreenProperty());

        StackPane pane = new StackPane(zoomedView);
        pane.setStyle("-fx-background-color: #080B10;");

        zoomedView.fitWidthProperty().bind(pane.widthProperty().subtract(20));
        zoomedView.fitHeightProperty().bind(pane.heightProperty().subtract(20));

        Scene scene = new Scene(pane, 960, 540);
        var cssUrl = getClass().getResource("/com/fiwpr06/lanmonitorfx/css/theme.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        zoomStage.setScene(scene);
        zoomStage.show();
    }
}
