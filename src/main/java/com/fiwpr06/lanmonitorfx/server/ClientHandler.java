package com.fiwpr06.lanmonitorfx.server;

import com.fiwpr06.lanmonitorfx.model.ClientSession;
import com.fiwpr06.lanmonitorfx.network.Message;
import com.fiwpr06.lanmonitorfx.network.Protocol;
import com.fiwpr06.lanmonitorfx.network.SocketConnection;
import com.fiwpr06.lanmonitorfx.util.FileLogger;
import com.fiwpr06.lanmonitorfx.util.ImageUtil;
import javafx.application.Platform;
import javafx.scene.image.Image;

import java.io.EOFException;
import java.net.SocketException;
import java.util.function.Consumer;

// Luồng chuyên trách xử lý đọc và phản hồi lệnh từ một Client cụ thể.
public class ClientHandler extends Thread {

    private final SocketConnection connection;
    private final ClientRegistry registry;
    private final Consumer<ClientSession> onClientConnected;
    private ClientSession session;

    public ClientHandler(SocketConnection connection, ClientRegistry registry, Consumer<ClientSession> onClientConnected) {
        this.connection = connection;
        this.registry = registry;
        this.onClientConnected = onClientConnected;
        setDaemon(true);
    }

    @Override
    public void run() {
        String clientInfo = connection.getRemoteAddress();

        try {
            // 1. Gói tin đầu tiên bắt buộc phải là LOGIN
            String initialMessage = connection.receive();
            Message loginMsg = Protocol.parse(initialMessage);

            if (!Protocol.CMD_LOGIN.equals(loginMsg.command())) {
                System.err.println("Gói tin đầu tiên không hợp lệ: " + initialMessage);
                return;
            }

            // LOGIN###mssv###hoten
            String mssv = loginMsg.param(0);
            String hoTen = loginMsg.param(1);
            String ip = connection.getRemoteAddress();
            String clientId = ip + "_" + mssv;
            clientInfo = hoTen + " (" + mssv + " - " + ip + ")";

            // Khởi tạo phiên kết nối
            session = new ClientSession(clientId, mssv, hoTen, ip, connection);
            registry.register(session);

            FileLogger.log("🟢 Sinh viên " + hoTen + " (MSSV: " + mssv + ", IP: " + ip + ") đã ĐĂNG NHẬP.");

            if (onClientConnected != null) {
                onClientConnected.accept(session);
            }

            // 2. Vòng lặp nhận dữ liệu từ Client
            while (connection.isConnected()) {
                String rawMsg = connection.receive();
                Message msg = Protocol.parse(rawMsg);

                switch (msg.command()) {
                    case Protocol.CMD_SCREEN_DATA -> {
                        // SCREEN_DATA###clientId###base64Image
                        String base64 = msg.param(1);
                        if (!base64.isBlank()) {
                            Image fxImage = ImageUtil.fromBase64(base64);
                            if (fxImage != null) {
                                Platform.runLater(() -> session.setLastScreen(fxImage));
                            }
                        }
                    }
                    case Protocol.CMD_DISCONNECT -> {
                        return; // Thoát vòng lặp
                    }
                    default -> {
                        // Bỏ qua hoặc log lệnh không rõ
                    }
                }
            }

        } catch (EOFException | SocketException e) {
            // Client đột ngột ngắt kết nối (tắt máy, đóng ứng dụng)
        } catch (Exception e) {
            System.err.println("Lỗi xử lý Client " + clientInfo + ": " + e.getMessage());
        } finally {
            if (session != null) {
                registry.markOffline(session.getClientId());
                FileLogger.log("🔴 Sinh viên " + session.getHoTen() + " đã THOÁT (Ngắt kết nối).");
            }
            connection.close();
        }
    }
}
