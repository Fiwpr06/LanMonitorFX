package com.fiwpr06.lanmonitorfx.ui;

import com.fiwpr06.lanmonitorfx.client.CommandExecutor;
import com.fiwpr06.lanmonitorfx.client.ScreenCaptureService;
import com.fiwpr06.lanmonitorfx.client.TcpClient;
import com.fiwpr06.lanmonitorfx.network.Protocol;
import com.fiwpr06.lanmonitorfx.util.AlertHelper;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;

/**
 * Controller điều khiển giao diện Sinh viên phong cách AnyDesk hiện đại.
 * Hỗ trợ hiển thị bảng điều khiển "This Desk", phiên kết nối trực tiếp "Remote Server",
 * đếm thời gian phiên học Uptime, và Hộp thư thông báo thời gian thực từ Giáo viên.
 */
public class ClientMainController {

    // --- TOP BAR ---
    @FXML private Label lblGlobalStatus;
    @FXML private Label lblMyIp;
    @FXML private Button btnCopyMyIp;

    // --- THẺ 1: THIS DESK ---
    @FXML private Label lblDeskId;
    @FXML private Label lblDeskName;
    @FXML private Label lblDeskMssv;
    @FXML private Label lblDeskHostname;

    // --- THẺ 2: SERVER CONNECTION ---
    @FXML private VBox paneConnectForm;
    @FXML private TextField txtServerIp;
    @FXML private TextField txtMssv;
    @FXML private TextField txtName;
    @FXML private Button btnConnect;
    @FXML private Label lblConnectStatus;

    @FXML private VBox paneActiveSession;
    @FXML private Label lblConnectedServer;
    @FXML private Label lblSessionTimer;
    @FXML private Label lblStreamStatus;
    @FXML private Label lblFrameCount;
    @FXML private Button btnDisconnect;

    // --- THẺ 3: NOTIFICATION FEED & SYSTEM SPECS ---
    @FXML private Label lblNoticeCount;
    @FXML private VBox noticeBox;
    @FXML private Label lblNoNotices;

    // --- TAB 2: THÔNG TIN MÁY TÍNH ---
    @FXML private Label lblOsInfo;
    @FXML private Label lblResolutionInfo;
    @FXML private Label lblHostInfo;
    @FXML private Label lblJavaInfo;

    // --- FOOTER ---
    @FXML private Label lblFooterStatus;

    // --- BACKEND SERVICES ---
    private TcpClient tcpClient;
    private ScreenCaptureService captureService;
    private CommandExecutor commandExecutor;

    private Timeline sessionTimeline;
    private int sessionSeconds = 0;
    private int noticeCount = 0;

    @FXML
    public void initialize() {
        initDeskIdentity();
    }

    /**
     * Tự động dò tìm IP LAN và Hostname của máy sinh viên để hiển thị lên thẻ "This Desk".
     */
    private void initDeskIdentity() {
        String myIp = detectLocalIp();
        String hostname = "DESKTOP-LAN";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {}

        lblMyIp.setText(myIp);
        lblDeskId.setText(myIp);
        lblDeskHostname.setText(hostname);

        initSystemSpecs(hostname);
    }

    private void initSystemSpecs(String hostname) {
        if (lblOsInfo != null) {
            String os = System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")";
            lblOsInfo.setText(os);
        }
        if (lblHostInfo != null) {
            lblHostInfo.setText(hostname);
        }
        if (lblResolutionInfo != null) {
            try {
                java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
                lblResolutionInfo.setText((int) screenSize.getWidth() + " x " + (int) screenSize.getHeight());
            } catch (Exception e) {
                lblResolutionInfo.setText("1920 x 1080");
            }
        }
        if (lblJavaInfo != null) {
            String javaVer = "Java " + System.getProperty("java.version");
            lblJavaInfo.setText(javaVer + " (JavaFX 21)");
        }
    }

    @FXML
    private void onSelectLocalhost() {
        txtServerIp.setText("127.0.0.1");
    }

    @FXML
    private void onSelectDetectedIp() {
        txtServerIp.setText(detectLocalIp());
    }

    /**
     * Dò tìm IPv4 nội mạng chính xác nhất.
     */
    public static String detectLocalIp() {
        try {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                String ip = socket.getLocalAddress().getHostAddress();
                if (ip != null && !ip.isBlank() && !ip.startsWith("127.") && !ip.equals("0.0.0.0")) {
                    return ip;
                }
            } catch (Exception ignored) {}

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    @FXML
    private void onCopyMyIp() {
        String ip = lblMyIp.getText();
        ServerMainController.copyToClipboard(ip);
        btnCopyMyIp.setText("✅ Đã chép!");
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> btnCopyMyIp.setText("📋 Copy"));
        pause.play();
    }

    @FXML
    private void onConnect() {
        String serverIp = txtServerIp.getText().trim();
        String mssv = txtMssv.getText().trim();
        String name = txtName.getText().trim();

        if (serverIp.isEmpty() || mssv.isEmpty() || name.isEmpty()) {
            AlertHelper.warn("Thiếu thông tin", "Vui lòng nhập đầy đủ IP Server, Mã SV và Họ Tên!");
            return;
        }

        btnConnect.setDisable(true);
        lblConnectStatus.setText("Đang kết nối tới " + serverIp + "...");
        lblConnectStatus.setStyle("-fx-text-fill: #00D2E6; -fx-font-weight: bold;");

        new Thread(() -> {
            commandExecutor = new CommandExecutor();
            // Lắng nghe thông báo để hiển thị vào Feed AnyDesk
            commandExecutor.setNotificationListener(this::addNotificationItem);

            tcpClient = new TcpClient(msg -> commandExecutor.execute(msg));
            boolean success = tcpClient.connect(serverIp, mssv, name);

            Platform.runLater(() -> {
                btnConnect.setDisable(false);
                if (success) {
                    onConnectSuccess(serverIp, mssv, name);
                } else {
                    lblConnectStatus.setText("🔴 Kết nối thất bại! Hãy kiểm tra IP Server.");
                    lblConnectStatus.setStyle("-fx-text-fill: #FF3B56; -fx-font-weight: bold;");
                    AlertHelper.warn("Kết nối thất bại", "Không thể kết nối tới Server tại " + serverIp + ".\nHãy đảm bảo Server giáo viên đã bật!");
                }
            });
        }, "Client-Connector").start();
    }

    private void onConnectSuccess(String serverIp, String mssv, String name) {
        // Khởi động dịch vụ chụp ảnh màn hình
        captureService = new ScreenCaptureService(tcpClient);
        captureService.start();

        // Cập nhật thông tin sinh viên trên thẻ This Desk
        lblDeskName.setText(name);
        lblDeskMssv.setText(mssv);

        // Chuyển đổi giao diện sang chế độ Active Session
        paneConnectForm.setVisible(false);
        paneConnectForm.setManaged(false);
        paneActiveSession.setVisible(true);
        paneActiveSession.setManaged(true);

        lblConnectedServer.setText(serverIp + ":" + Protocol.PORT);
        lblGlobalStatus.setText("● Đang được giám sát");
        lblGlobalStatus.getStyleClass().clear();
        lblGlobalStatus.getStyleClass().add("status-badge-online");

        lblFooterStatus.setText("🟢 Đã kết nối thành công tới Server " + serverIp);

        // Bắt đầu đồng hồ đếm thời gian phiên học
        startSessionTimer();

        // Ghi lại thông báo chào mừng trong app
        addNotificationItem("Chào mừng " + name + " [" + mssv + "] đã tham gia buổi học!");
    }

    @FXML
    private void onDisconnect() {
        boolean confirmed = AlertHelper.confirm("Xác nhận ngắt kết nối", "Bạn có chắc chắn muốn ngắt kết nối khỏi lớp học không?");
        if (!confirmed) return;

        cleanupServices();

        // Chuyển giao diện về form kết nối
        paneActiveSession.setVisible(false);
        paneActiveSession.setManaged(false);
        paneConnectForm.setVisible(true);
        paneConnectForm.setManaged(true);

        lblGlobalStatus.setText("● Chưa kết nối");
        lblGlobalStatus.getStyleClass().clear();
        lblGlobalStatus.getStyleClass().add("status-badge-offline");

        lblConnectStatus.setText("Đã ngắt kết nối khỏi lớp học.");
        lblConnectStatus.setStyle("-fx-text-fill: #64748B;");

        lblFooterStatus.setText("⚪ Trạng thái hệ thống: Sẵn sàng kết nối");
    }

    private void startSessionTimer() {
        sessionSeconds = 0;
        if (sessionTimeline != null) {
            sessionTimeline.stop();
        }

        sessionTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            sessionSeconds++;
            int h = sessionSeconds / 3600;
            int m = (sessionSeconds % 3600) / 60;
            int s = sessionSeconds % 60;
            lblSessionTimer.setText(String.format("%02d:%02d:%02d", h, m, s));

            if (captureService != null) {
                lblFrameCount.setText(captureService.getFrameCount() + " khung hình");
            }
        }));
        sessionTimeline.setCycleCount(Animation.INDEFINITE);
        sessionTimeline.play();
    }

    /**
     * Thêm một thẻ thông báo vào Hộp thư thông báo AnyDesk trên giao diện.
     * Tự động phân biệt giữa [GỬI RIÊNG] và [CẢ LỚP] bằng huy hiệu và màu sắc trực quan.
     */
    public void addNotificationItem(String message) {
        Platform.runLater(() -> {
            if (lblNoNotices != null && noticeBox.getChildren().contains(lblNoNotices)) {
                noticeBox.getChildren().remove(lblNoNotices);
            }

            noticeCount++;
            lblNoticeCount.setText("(" + noticeCount + " thông báo)");

            String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            boolean isPrivate = message.startsWith("[GỬI RIÊNG]")
                    || message.contains("[TIN NHẮN RIÊNG]")
                    || message.contains("[GỬI RIÊNG NHÓM");

            String badgeText = isPrivate ? "🔒 TIN RIÊNG" : "📢 CẢ LỚP";
            String badgeBg = isPrivate ? "#3D2406" : "#0A2540";
            String badgeFg = isPrivate ? "#F59E0B" : "#00D2E6";
            String cardClass = isPrivate ? "notice-card-private" : "notice-card";

            HBox card = new HBox(8);
            card.setAlignment(Pos.CENTER_LEFT);
            card.getStyleClass().add(cardClass);
            card.setPadding(new Insets(8, 12, 8, 12));

            Label badgeLbl = new Label(badgeText);
            badgeLbl.setStyle("-fx-background-color: " + badgeBg + "; -fx-text-fill: " + badgeFg + "; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 3px 6px; -fx-background-radius: 4px;");

            Label timeLbl = new Label("[" + timeStr + "]");
            timeLbl.setStyle("-fx-font-family: 'Consolas', monospace; -fx-text-fill: #94A3B8; -fx-font-weight: bold; -fx-font-size: 11px;");

            Label msgLbl = new Label(message);
            msgLbl.setWrapText(true);
            msgLbl.setStyle("-fx-font-family: 'Segoe UI', Arial; -fx-text-fill: #F0F4F8; -fx-font-size: 13px;");
            HBox.setHgrow(msgLbl, Priority.ALWAYS);

            card.getChildren().addAll(badgeLbl, timeLbl, msgLbl);

            // Thêm vào đầu danh sách (tin mới nhất lên trên)
            noticeBox.getChildren().add(0, card);
        });
    }

    @FXML
    private void onClearNotices() {
        noticeBox.getChildren().clear();
        noticeCount = 0;
        lblNoticeCount.setText("(0 thông báo)");
        if (lblNoNotices != null) {
            noticeBox.getChildren().add(lblNoNotices);
        }
    }

    private void cleanupServices() {
        if (sessionTimeline != null) {
            sessionTimeline.stop();
            sessionTimeline = null;
        }
        if (captureService != null) {
            captureService.stop();
            captureService = null;
        }
        if (commandExecutor != null) {
            commandExecutor.cleanup();
        }
        if (tcpClient != null) {
            tcpClient.disconnect();
            tcpClient = null;
        }
    }

    public void shutdown() {
        cleanupServices();
    }
}
