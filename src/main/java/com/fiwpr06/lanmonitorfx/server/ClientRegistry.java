package com.fiwpr06.lanmonitorfx.server;

import com.fiwpr06.lanmonitorfx.model.ClientSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Quản lý danh sách các phiên kết nối Client một cách an toàn đa luồng (Thread-safe).
 * - Sử dụng CopyOnWriteArrayList để các luồng mạng có thể duyệt, tìm kiếm, gửi tin đồng thời.
 * - Tự động cập nhật SocketConnection sống khi sinh viên kết nối lại (reconnect).
 * - Sử dụng ObservableList để giao diện JavaFX (TableView, FlowPane) liên kết và tự động cập nhật.
 */
public class ClientRegistry {

    private final CopyOnWriteArrayList<ClientSession> activeSessions = new CopyOnWriteArrayList<>();
    private final ObservableList<ClientSession> observableClients = FXCollections.observableArrayList();
    private final ObservableList<ClientSession> unmodifiableClients = FXCollections.unmodifiableObservableList(observableClients);

    // Trả về danh sách chỉ đọc để giao diện JavaFX liên kết trực tiếp trên JavaFX Thread.
    public ObservableList<ClientSession> getClients() {
        return unmodifiableClients;
    }

    /**
     * Đăng ký hoặc tái kích hoạt một phiên kết nối Client.
     * Cập nhật Socket mới ngay lập tức vào danh sách mạng và đồng bộ sang UI bằng Platform.runLater().
     */
    public void register(ClientSession session) {
        // Cập nhật trên danh sách mạng (Thread-safe)
        Optional<ClientSession> existing = activeSessions.stream()
                .filter(c -> c.getMssv().equalsIgnoreCase(session.getMssv()))
                .findFirst();

        if (existing.isPresent()) {
            ClientSession old = existing.get();
            // Cập nhật SocketConnection sống mới nhất để bảo đảm các lệnh NOTIFY, SHUTDOWN tới được máy sinh viên
            old.updateConnection(session.getConnection(), session.getIpAddress(), session.getClientId());
            old.setHoTen(session.getHoTen());
        } else {
            activeSessions.add(session);
        }

        // Đồng bộ sang UI trên JavaFX Application Thread
        Platform.runLater(() -> {
            Optional<ClientSession> uiExisting = observableClients.stream()
                    .filter(c -> c.getMssv().equalsIgnoreCase(session.getMssv()))
                    .findFirst();

            if (uiExisting.isPresent()) {
                ClientSession old = uiExisting.get();
                old.updateConnection(session.getConnection(), session.getIpAddress(), session.getClientId());
                old.setHoTen(session.getHoTen());
            } else {
                observableClients.add(session);
            }
        });
    }

    // Đánh dấu một Client chuyển sang trạng thái Offline khi ngắt kết nối.
    public void markOffline(String clientId) {
        for (ClientSession session : activeSessions) {
            if (session.getClientId().equals(clientId) || session.getMssv().equalsIgnoreCase(clientId)) {
                session.setStatus("Offline");
                session.setWarning(false);
                break;
            }
        }

        Platform.runLater(() -> {
            for (ClientSession session : observableClients) {
                if (session.getClientId().equals(clientId) || session.getMssv().equalsIgnoreCase(clientId)) {
                    session.setStatus("Offline");
                    session.setWarning(false);
                    break;
                }
            }
        });
    }

    // Tìm kiếm một phiên Client theo clientId hoặc MSSV một cách an toàn luồng.
    public Optional<ClientSession> findById(String identifier) {
        return activeSessions.stream()
                .filter(c -> c.getClientId().equals(identifier) || c.getMssv().equalsIgnoreCase(identifier))
                .findFirst();
    }

    /**
     * Gửi thông điệp tới TẤT CẢ các Client đang Online.
     * Duyệt an toàn qua CopyOnWriteArrayList, không làm chặn hay xung đột với UI thread.
     */
    public void sendToAll(String message) {
        for (ClientSession session : activeSessions) {
            if ("Online".equals(session.getStatus()) && session.getConnection() != null && session.getConnection().isConnected()) {
                try {
                    session.getConnection().send(message);
                } catch (Exception e) {
                    System.err.println("Lỗi gửi lệnh tới client " + session.getHoTen() + ": " + e.getMessage());
                }
            }
        }
    }

    // Gửi thông điệp tới một Client chỉ định theo clientId hoặc MSSV.
    public void sendTo(String identifier, String message) {
        findById(identifier).ifPresent(session -> {
            if (session.getConnection() != null && session.getConnection().isConnected()) {
                try {
                    session.getConnection().send(message);
                } catch (Exception e) {
                    System.err.println("Lỗi gửi lệnh tới client " + identifier + ": " + e.getMessage());
                }
            }
        });
    }

    // Đếm số lượng máy đang kết nối Online.
    public long getOnlineCount() {
        return activeSessions.stream().filter(c -> "Online".equals(c.getStatus())).count();
    }

    // Thiết lập trạng thái cảnh báo / khóa cho toàn bộ client Online.
    public void setAllWarning(boolean warning) {
        for (ClientSession session : activeSessions) {
            if ("Online".equals(session.getStatus())) {
                session.setWarning(warning);
            }
        }
    }
}
