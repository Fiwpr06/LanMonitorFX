package com.fiwpr06.lanmonitorfx.ui;

import com.fiwpr06.lanmonitorfx.model.ClientSession;
import com.fiwpr06.lanmonitorfx.network.Protocol;
import com.fiwpr06.lanmonitorfx.server.ClientRegistry;
import com.fiwpr06.lanmonitorfx.server.TcpServer;
import com.fiwpr06.lanmonitorfx.util.AlertHelper;
import com.fiwpr06.lanmonitorfx.util.FileLogger;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller chính quản lý bảng điều khiển phía Giáo viên (Dashboard).
 * Hỗ trợ:
 * - Hiển thị và sao chép IP máy chủ (Server IP).
 * - Quản lý máy tính (Tắt máy / Khởi động lại / Đăng xuất riêng lẻ, theo nhóm chọn hoặc toàn bộ cả lớp).
 * - Thanh thao tác theo nhóm máy được chọn (Google Drive Style Contextual Action Bar).
 * - Xem nhật ký trực tiếp (Live Log Tab) đồng bộ thời gian thực UTF-8.
 * - Menu ngữ cảnh (Right-click) trên từng sinh viên để thao tác nhanh.
 */
public class ServerMainController {

    @FXML private TabPane tabPane;
    @FXML private Tab tabLog;

    // --- GOOGLE DRIVE-STYLE SELECTION ACTION BAR ---
    @FXML private HBox selectionBar;
    @FXML private Label lblSelectedCount;
    @FXML private Button btnActionNotify;
    @FXML private Button btnActionLock;
    @FXML private Button btnActionUnlock;
    @FXML private Button btnActionShutdown;
    @FXML private Button btnActionRestart;

    // --- TABLE VIEW ---
    @FXML private TableView<ClientSession> clientTable;
    @FXML private TableColumn<ClientSession, Boolean> colSelect;
    @FXML private TableColumn<ClientSession, Number> colIndex;
    @FXML private TableColumn<ClientSession, String> colMssv;
    @FXML private TableColumn<ClientSession, String> colName;
    @FXML private TableColumn<ClientSession, String> colIp;
    @FXML private TableColumn<ClientSession, String> colStatus;

    @FXML private FlowPane monitorGrid;
    @FXML private Label statusBar;

    @FXML private Label lblServerIp;
    @FXML private Button btnCopyIp;

    @FXML private TextArea txtLiveLog;
    @FXML private Label lblLogStatus;

    private ClientRegistry registry;
    private TcpServer tcpServer;
    private final Map<String, Node> cardNodes = new HashMap<>();

    @FXML
    public void initialize() {
        setupTableColumns();
        setupTableRowContextMenu();
        initServerIpDisplay();
        initLiveLogTab();
    }

    private void initServerIpDisplay() {
        String ip = detectLocalLanIp();
        if (lblServerIp != null) {
            lblServerIp.setText(ip);
        }
    }

    public static String detectLocalLanIp() {
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
    private void onCopyServerIp() {
        String ip = lblServerIp != null ? lblServerIp.getText().trim() : detectLocalLanIp();
        copyToClipboard(ip);

        if (btnCopyIp != null) {
            btnCopyIp.setText("✅ Đã chép!");
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(e -> btnCopyIp.setText("📋 Copy IP"));
            pause.play();
        }

        setStatus("✅ Đã sao chép IP máy chủ (" + ip + ") vào bộ nhớ tạm!");
    }

    public static void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    private void initLiveLogTab() {
        if (txtLiveLog != null) {
            txtLiveLog.setText(FileLogger.readTodayLog());
            txtLiveLog.positionCaret(txtLiveLog.getLength());

            FileLogger.addListener(logLine -> Platform.runLater(() -> {
                txtLiveLog.appendText(logLine);
                txtLiveLog.positionCaret(txtLiveLog.getLength());
            }));
        }
    }

    public void initServer() {
        registry = new ClientRegistry();
        clientTable.setItems(registry.getClients());

        registry.getClients().addListener((ListChangeListener<ClientSession>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (ClientSession addedSession : change.getAddedSubList()) {
                        addClientCard(addedSession);
                        addedSession.selectedProperty().addListener((obs, oldVal, newVal) -> updateSelectionBar());
                    }
                }
                if (change.wasRemoved()) {
                    for (ClientSession removedSession : change.getRemoved()) {
                        removeClientCard(removedSession);
                    }
                }
            }
            updateStatusBar();
            updateSelectionBar();
        });

        tcpServer = new TcpServer(Protocol.PORT, registry, session -> {
            Platform.runLater(this::updateStatusBar);
        });
        tcpServer.start();

        updateStatusBar();
        updateSelectionBar();
    }

    private void setupTableColumns() {
        clientTable.setEditable(true);
        clientTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        if (colSelect != null) {
            CheckBox selectAll = new CheckBox();
            selectAll.setOnAction(e -> {
                boolean val = selectAll.isSelected();
                if (registry != null) {
                    for (ClientSession c : registry.getClients()) {
                        c.setSelected(val);
                    }
                }
            });
            colSelect.setGraphic(selectAll);
            colSelect.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
            colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
            colSelect.setEditable(true);
        }

        colIndex.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(clientTable.getItems().indexOf(cellData.getValue()) + 1));
        colIndex.setSortable(false);

        colMssv.setCellValueFactory(data -> data.getValue().mssvProperty());
        colName.setCellValueFactory(data -> data.getValue().hoTenProperty());
        colIp.setCellValueFactory(data -> data.getValue().ipAddressProperty());
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    if ("Online".equalsIgnoreCase(item)) {
                        setText("● Online");
                        setStyle("-fx-text-fill: #2EE68A; -fx-font-weight: bold;");
                    } else {
                        setText("○ Offline");
                        setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void setupTableRowContextMenu() {
        clientTable.setRowFactory(tv -> {
            TableRow<ClientSession> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();

            MenuItem copyIpItem = new MenuItem("📋 Sao chép IP");
            copyIpItem.setOnAction(e -> {
                ClientSession item = row.getItem();
                if (item != null) {
                    copyToClipboard(item.getIpAddress());
                    setStatus("📋 Đã sao chép IP (" + item.getIpAddress() + ") của sinh viên " + item.getHoTen());
                }
            });

            MenuItem sendNotifyItem = new MenuItem("💬 Gửi thông báo riêng");
            sendNotifyItem.setOnAction(e -> {
                ClientSession item = row.getItem();
                if (item != null) sendNotifyToClient(item);
            });

            MenuItem lockItem = new MenuItem("🔒 Khóa máy này");
            lockItem.setOnAction(e -> {
                ClientSession item = row.getItem();
                if (item != null) {
                    registry.sendTo(item.getClientId(), Protocol.CMD_LOCK);
                    item.setWarning(true);
                    FileLogger.log("🔒 Giáo viên đã phát lệnh KHÓA máy sinh viên: " + item.getHoTen());
                }
            });

            MenuItem unlockItem = new MenuItem("🔓 Mở khóa máy này");
            unlockItem.setOnAction(e -> {
                ClientSession item = row.getItem();
                if (item != null) {
                    registry.sendTo(item.getClientId(), Protocol.CMD_UNLOCK);
                    item.setWarning(false);
                    FileLogger.log("🔓 Giáo viên đã phát lệnh MỞ KHÓA máy sinh viên: " + item.getHoTen());
                }
            });

            MenuItem shutdownItem = new MenuItem("⚡ Tắt máy này");
            shutdownItem.setOnAction(e -> {
                ClientSession item = row.getItem();
                if (item != null) shutdownSingleClient(item);
            });

            MenuItem restartItem = new MenuItem("🔄 Khởi động lại máy này");
            restartItem.setOnAction(e -> {
                ClientSession item = row.getItem();
                if (item != null) restartSingleClient(item);
            });

            contextMenu.getItems().addAll(
                    copyIpItem,
                    new SeparatorMenuItem(),
                    sendNotifyItem,
                    lockItem,
                    unlockItem,
                    new SeparatorMenuItem(),
                    shutdownItem,
                    restartItem
            );

            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );

            return row;
        });
    }

    private void addClientCard(ClientSession session) {
        if (cardNodes.containsKey(session.getClientId())) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fiwpr06/lanmonitorfx/fxml/client_card.fxml"));
            Node cardNode = loader.load();
            ClientCardController controller = loader.getController();
            controller.init(session, this);

            cardNodes.put(session.getClientId(), cardNode);
            monitorGrid.getChildren().add(cardNode);
        } catch (IOException e) {
            System.err.println("Lỗi tạo thẻ giám sát: " + e.getMessage());
        }
    }

    private void removeClientCard(ClientSession session) {
        Node node = cardNodes.remove(session.getClientId());
        if (node != null) {
            monitorGrid.getChildren().remove(node);
        }
    }

    private void updateStatusBar() {
        long count = registry != null ? registry.getOnlineCount() : 0;
        String ip = lblServerIp != null ? lblServerIp.getText() : detectLocalLanIp();
        statusBar.setText("Đang lắng nghe cổng " + Protocol.PORT + "   |   🌐 IP Server: " + ip + "   |   🟢 Sĩ số lớp: " + count + " máy tính đang kết nối");
    }

    public void setStatus(String text) {
        if (statusBar != null) {
            statusBar.setText(text);
        }
    }

    // --- GOOGLE DRIVE-STYLE CONTEXTUAL SELECTION MANAGEMENT ---

    public void updateSelectionBar() {
        Platform.runLater(() -> {
            if (selectionBar == null || registry == null) return;
            List<ClientSession> selected = registry.getClients().stream()
                    .filter(ClientSession::isSelected)
                    .toList();
            int count = selected.size();
            if (count > 0) {
                selectionBar.setVisible(true);
                selectionBar.setManaged(true);
                if (lblSelectedCount != null) {
                    lblSelectedCount.setText("✓ Đã chọn " + count + " máy tính");
                }
                if (btnActionNotify != null) btnActionNotify.setText("💬 Gửi thông báo (" + count + ")");
                if (btnActionLock != null) btnActionLock.setText("🔒 Khóa (" + count + ")");
                if (btnActionUnlock != null) btnActionUnlock.setText("🔓 Mở khóa (" + count + ")");
                if (btnActionShutdown != null) btnActionShutdown.setText("⚡ Tắt máy (" + count + ")");
                if (btnActionRestart != null) btnActionRestart.setText("🔄 Khởi động lại (" + count + ")");
            } else {
                selectionBar.setVisible(false);
                selectionBar.setManaged(false);
            }
        });
    }

    public List<ClientSession> getTargetClients() {
        if (registry == null) return List.of();
        List<ClientSession> checked = registry.getClients().stream().filter(ClientSession::isSelected).toList();
        if (!checked.isEmpty()) {
            return checked;
        }
        ClientSession tableSelected = clientTable.getSelectionModel().getSelectedItem();
        if (tableSelected != null) {
            return List.of(tableSelected);
        }
        return List.of();
    }

    @FXML
    private void onClearSelection() {
        if (registry != null) {
            for (ClientSession c : registry.getClients()) {
                c.setSelected(false);
            }
        }
        clientTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void onNotifySelected() {
        List<ClientSession> targets = getTargetClients();
        if (targets.isEmpty()) {
            AlertHelper.warn("Chưa chọn máy tính", "Vui lòng chọn ít nhất một máy tính cần gửi thông báo!");
            return;
        }

        Optional<String> msgOpt = AlertHelper.textInput(
                "Gửi thông báo (" + targets.size() + " máy tính)",
                "Nhập nội dung gửi tới " + targets.size() + " máy tính đã chọn:"
        );
        msgOpt.ifPresent(msg -> {
            if (!msg.isBlank()) {
                String payload = targets.size() == 1
                        ? Protocol.buildPrivateNotify(msg.trim())
                        : Protocol.buildGroupNotify(targets.size(), msg.trim());
                for (ClientSession c : targets) {
                    registry.sendTo(c.getClientId(), payload);
                }
                FileLogger.log("🔒 Giáo viên gửi thông báo tới " + targets.size() + " máy tính đã chọn: " + msg.trim());
                setStatus("💬 Đã gửi thông báo tới " + targets.size() + " máy tính đã chọn!");
            }
        });
    }

    @FXML
    private void onLockSelected() {
        List<ClientSession> targets = getTargetClients();
        if (targets.isEmpty()) {
            AlertHelper.warn("Chưa chọn máy tính", "Vui lòng chọn ít nhất một máy tính cần khóa!");
            return;
        }
        for (ClientSession c : targets) {
            registry.sendTo(c.getClientId(), Protocol.CMD_LOCK);
            c.setWarning(true);
        }
        FileLogger.log("🔒 Giáo viên đã phát lệnh KHÓA " + targets.size() + " máy tính đã chọn.");
        setStatus("🔒 Đã gửi lệnh KHÓA tới " + targets.size() + " máy tính đã chọn!");
    }

    @FXML
    private void onUnlockSelected() {
        List<ClientSession> targets = getTargetClients();
        if (targets.isEmpty()) {
            AlertHelper.warn("Chưa chọn máy tính", "Vui lòng chọn ít nhất một máy tính cần mở khóa!");
            return;
        }
        for (ClientSession c : targets) {
            registry.sendTo(c.getClientId(), Protocol.CMD_UNLOCK);
            c.setWarning(false);
        }
        FileLogger.log("🔓 Giáo viên đã phát lệnh MỞ KHÓA " + targets.size() + " máy tính đã chọn.");
        setStatus("🔓 Đã gửi lệnh MỞ KHÓA tới " + targets.size() + " máy tính đã chọn!");
    }

    @FXML
    private void onShutdownSelected() {
        List<ClientSession> targets = getTargetClients();
        if (targets.isEmpty()) {
            AlertHelper.warn("Chưa chọn máy tính", "Vui lòng chọn ít nhất một máy tính cần tắt nguồn!");
            return;
        }
        boolean confirmed = AlertHelper.confirm(
                "Xác nhận tắt máy",
                "⚡ Bạn có chắc chắn muốn TẮT NGUỒN " + targets.size() + " máy tính đã chọn?"
        );
        if (confirmed) {
            for (ClientSession c : targets) {
                registry.sendTo(c.getClientId(), Protocol.CMD_SHUTDOWN);
            }
            FileLogger.log("⚡ Giáo viên đã phát lệnh TẮT NGUỒN " + targets.size() + " máy tính đã chọn.");
            setStatus("⚡ Đã gửi lệnh tắt nguồn tới " + targets.size() + " máy tính đã chọn!");
        }
    }

    @FXML
    private void onRestartSelected() {
        List<ClientSession> targets = getTargetClients();
        if (targets.isEmpty()) {
            AlertHelper.warn("Chưa chọn máy tính", "Vui lòng chọn ít nhất một máy tính cần khởi động lại!");
            return;
        }
        boolean confirmed = AlertHelper.confirm(
                "Xác nhận khởi động lại",
                "🔄 Bạn có chắc chắn muốn KHỞI ĐỘNG LẠI " + targets.size() + " máy tính đã chọn?"
        );
        if (confirmed) {
            for (ClientSession c : targets) {
                registry.sendTo(c.getClientId(), Protocol.CMD_RESTART);
            }
            FileLogger.log("🔄 Giáo viên đã phát lệnh KHỞI ĐỘNG LẠI " + targets.size() + " máy tính đã chọn.");
            setStatus("🔄 Đã gửi lệnh khởi động lại tới " + targets.size() + " máy tính đã chọn!");
        }
    }

    // --- CÁC HÀNH ĐỘNG TOÀN CỤC CẢ LỚP (GLOBAL TOOLBAR) ---

    @FXML
    private void onLock() {
        if (ensureClientsConnected()) {
            registry.sendToAll(Protocol.CMD_LOCK);
            registry.setAllWarning(true);
            FileLogger.log("🔒 Giáo viên đã phát lệnh KHÓA TOÀN BỘ máy tính trong lớp.");
            AlertHelper.info("Thành công", "Đã gửi lệnh KHÓA màn hình tới tất cả máy tính!");
        }
    }

    @FXML
    private void onUnlock() {
        if (ensureClientsConnected()) {
            registry.sendToAll(Protocol.CMD_UNLOCK);
            registry.setAllWarning(false);
            FileLogger.log("🔓 Giáo viên đã phát lệnh MỞ KHÓA TOÀN BỘ máy tính trong lớp.");
            AlertHelper.info("Thành công", "Đã gửi lệnh MỞ KHÓA màn hình tới tất cả máy tính!");
        }
    }

    @FXML
    private void onShutdownAll() {
        if (ensureClientsConnected() && AlertHelper.confirm("Xác nhận tắt toàn bộ", "⚡ Bạn có chắc chắn muốn TẮT NGUỒN TOÀN BỘ máy tính trong lớp?")) {
            registry.sendToAll(Protocol.CMD_SHUTDOWN);
            FileLogger.log("⚡ Giáo viên đã phát lệnh TẮT NGUỒN TOÀN BỘ máy tính trong lớp.");
            AlertHelper.info("Thành công", "Đã gửi lệnh tắt nguồn tới toàn bộ máy tính!");
        }
    }

    @FXML
    private void onRestartAll() {
        if (ensureClientsConnected() && AlertHelper.confirm("Xác nhận khởi động lại toàn bộ", "🔄 Bạn có chắc chắn muốn KHỞI ĐỘNG LẠI TOÀN BỘ máy tính trong lớp?")) {
            registry.sendToAll(Protocol.CMD_RESTART);
            FileLogger.log("🔄 Giáo viên đã phát lệnh KHỞI ĐỘNG LẠI TOÀN BỘ máy tính trong lớp.");
            AlertHelper.info("Thành công", "Đã gửi lệnh khởi động lại tới toàn bộ máy tính!");
        }
    }

    @FXML
    private void onLogoutAll() {
        if (ensureClientsConnected() && AlertHelper.confirm("Xác nhận đăng xuất toàn bộ", "🚪 Bạn có chắc chắn muốn ĐĂNG XUẤT TOÀN BỘ máy tính trong lớp?")) {
            registry.sendToAll(Protocol.CMD_LOGOUT);
            FileLogger.log("🚪 Giáo viên đã phát lệnh ĐĂNG XUẤT TOÀN BỘ máy tính trong lớp.");
            AlertHelper.info("Thành công", "Đã gửi lệnh đăng xuất tới toàn bộ máy tính!");
        }
    }

    @FXML
    private void onNotify() {
        if (!ensureClientsConnected()) return;

        Optional<String> msgOpt = AlertHelper.textInput("Gửi thông báo cả lớp", "Nhập nội dung thông báo gửi tới toàn bộ lớp học:");
        msgOpt.ifPresent(msg -> {
            if (!msg.isBlank()) {
                registry.sendToAll(Protocol.buildBroadcastNotify(msg.trim()));
                FileLogger.log("📢 Giáo viên gửi thông báo tới CẢ LỚP: " + msg.trim());
                AlertHelper.info("Thành công", "Đã gửi thông báo thành công tới cả lớp!");
            }
        });
    }

    // --- CÁC HÀNH ĐỘNG RIÊNG LẺ (MENU CHUỘT PHẢI) ---

    public void shutdownSingleClient(ClientSession session) {
        boolean confirmed = AlertHelper.confirm(
                "Xác nhận tắt máy",
                "Bạn có chắc chắn muốn TẮT NGUỒN máy tính của sinh viên:\n"
                        + session.getHoTen() + " (MSSV: " + session.getMssv() + " | IP: " + session.getIpAddress() + ")?"
        );
        if (confirmed) {
            registry.sendTo(session.getClientId(), Protocol.CMD_SHUTDOWN);
            FileLogger.log("⚡ Giáo viên đã phát lệnh TẮT NGUỒN máy tính: " + session.getHoTen() + " (" + session.getIpAddress() + ")");
            AlertHelper.info("Thành công", "Đã gửi lệnh tắt nguồn tới máy tính của " + session.getHoTen() + "!");
        }
    }

    public void restartSingleClient(ClientSession session) {
        boolean confirmed = AlertHelper.confirm(
                "Xác nhận khởi động lại",
                "Bạn có chắc chắn muốn KHỞI ĐỘNG LẠI máy tính của sinh viên:\n"
                        + session.getHoTen() + " (MSSV: " + session.getMssv() + " | IP: " + session.getIpAddress() + ")?"
        );
        if (confirmed) {
            registry.sendTo(session.getClientId(), Protocol.CMD_RESTART);
            FileLogger.log("🔄 Giáo viên đã phát lệnh KHỞI ĐỘNG LẠI máy tính: " + session.getHoTen() + " (" + session.getIpAddress() + ")");
            AlertHelper.info("Thành công", "Đã gửi lệnh khởi động lại tới máy tính của " + session.getHoTen() + "!");
        }
    }

    public void sendNotifyToClient(ClientSession session) {
        Optional<String> msgOpt = AlertHelper.textInput(
                "Gửi thông báo riêng",
                "Nhập nội dung gửi riêng tới " + session.getHoTen() + " (" + session.getIpAddress() + "):"
        );
        msgOpt.ifPresent(msg -> {
            if (!msg.isBlank()) {
                registry.sendTo(session.getClientId(), Protocol.buildPrivateNotify(msg.trim()));
                FileLogger.log("🔒 Giáo viên gửi thông báo RIÊNG tới " + session.getHoTen() + " [" + session.getMssv() + "]: " + msg.trim());
                AlertHelper.info("Thành công", "Đã gửi thông báo riêng thành công tới " + session.getHoTen() + "!");
            }
        });
    }

    // --- NHẬT KÝ HOẠT ĐỘNG (LOG VIEWER) ---

    @FXML
    private void onViewLog() {
        if (tabPane != null && tabLog != null) {
            tabPane.getSelectionModel().select(tabLog);
        }
        onRefreshLog();
    }

    @FXML
    private void onRefreshLog() {
        if (txtLiveLog != null) {
            txtLiveLog.setText(FileLogger.readTodayLog());
            txtLiveLog.positionCaret(txtLiveLog.getLength());
        }
        if (lblLogStatus != null) {
            lblLogStatus.setText("🟢 Đã làm mới lúc " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
    }

    @FXML
    private void onOpenLogFolder() {
        try {
            File logDir = new File("logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(logDir);
            } else {
                AlertHelper.info("Thư mục Log", "Đường dẫn thư mục log: " + logDir.getAbsolutePath());
            }
        } catch (Exception e) {
            AlertHelper.warn("Lỗi mở thư mục", "Không thể mở thư mục logs: " + e.getMessage());
        }
    }

    @FXML
    private void onClearLog() {
        boolean confirmed = AlertHelper.confirm("Xóa nhật ký", "Bạn có chắc chắn muốn XÓA TOÀN BỘ nhật ký hoạt động ngày hôm nay?");
        if (confirmed) {
            boolean success = FileLogger.clearTodayLog();
            if (success) {
                if (txtLiveLog != null) {
                    txtLiveLog.setText(FileLogger.readTodayLog());
                }
                AlertHelper.info("Thành công", "Đã xóa sạch nội dung nhật ký ngày hôm nay!");
            }
        }
    }

    private boolean ensureClientsConnected() {
        if (registry == null || registry.getOnlineCount() == 0) {
            AlertHelper.warn("Chưa có kết nối", "Hiện chưa có máy tính nào kết nối tới Server!");
            return false;
        }
        return true;
    }

    public ClientRegistry getRegistry() {
        return registry;
    }

    public void shutdown() {
        if (tcpServer != null) {
            tcpServer.stop();
        }
    }
}
