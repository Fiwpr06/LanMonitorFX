package com.fiwpr06.lanmonitorfx.client;

import com.fiwpr06.lanmonitorfx.network.Message;
import com.fiwpr06.lanmonitorfx.network.Protocol;
import com.fiwpr06.lanmonitorfx.network.SocketConnection;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

// Quản lý kết nối TCP Socket từ máy sinh viên đến Server của giáo viên.
public class TcpClient {

    private final Consumer<Message> onCommandReceived;
    private SocketConnection connection;
    private volatile boolean connected = false;
    private String clientId = "";

    public TcpClient(Consumer<Message> onCommandReceived) {
        this.onCommandReceived = onCommandReceived;
    }

    /**
     * Thực hiện kết nối tới Server và gửi thông tin đăng nhập.
     *
     * @param serverIp Địa chỉ IP của máy giáo viên
     * @param mssv     Mã số sinh viên
     * @param name     Họ và tên sinh viên
     * @return true nếu kết nối và gửi thông tin thành công
     */
    public synchronized boolean connect(String serverIp, String mssv, String name) {
        try {
            Socket socket = new Socket(serverIp, Protocol.PORT);
            this.connection = new SocketConnection(socket);
            this.clientId = connection.getRemoteAddress() + "_" + mssv;

            // Gửi gói tin LOGIN đầu tiên
            connection.send(Protocol.buildLogin(mssv, name));
            this.connected = true;

            // Bắt đầu lắng nghe lệnh điều khiển từ giáo viên
            startListening();
            return true;
        } catch (IOException e) {
            System.err.println("Lỗi kết nối tới Server: " + e.getMessage());
            disconnect();
            return false;
        }
    }

    private void startListening() {
        Thread listenThread = new Thread(() -> {
            try {
                while (connected && connection != null && connection.isConnected()) {
                    String rawMsg = connection.receive();
                    Message message = Protocol.parse(rawMsg);

                    if (onCommandReceived != null) {
                        onCommandReceived.accept(message);
                    }
                }
            } catch (Exception e) {
                if (connected) {
                    System.out.println("Mất kết nối tới Server: " + e.getMessage());
                }
            } finally {
                disconnect();
            }
        }, "TcpClient-Listener");

        listenThread.setDaemon(true);
        listenThread.start();
    }

    // Gửi dữ liệu ảnh chụp màn hình nén dạng Base64 về Server.
    public synchronized void sendScreenData(String base64Image) {
        if (!connected || connection == null || !connection.isConnected()) {
            return;
        }

        try {
            connection.send(Protocol.buildScreenData(clientId, base64Image));
        } catch (IOException e) {
            System.err.println("Lỗi gửi dữ liệu ảnh màn hình: " + e.getMessage());
        }
    }

    // Ngắt kết nối an toàn với Server.
    public synchronized void disconnect() {
        if (!connected) return;
        connected = false;

        try {
            if (connection != null && connection.isConnected()) {
                connection.send(Protocol.CMD_DISCONNECT);
            }
        } catch (Exception ignored) {}

        if (connection != null) {
            connection.close();
        }
    }

    public boolean isConnected() {
        return connected && connection != null && connection.isConnected();
    }

    public String getClientId() {
        return clientId;
    }
}
