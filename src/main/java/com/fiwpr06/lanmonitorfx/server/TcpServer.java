package com.fiwpr06.lanmonitorfx.server;

import com.fiwpr06.lanmonitorfx.model.ClientSession;
import com.fiwpr06.lanmonitorfx.network.SocketConnection;
import com.fiwpr06.lanmonitorfx.util.FileLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

// Quản lý ServerSocket TCP chạy ngầm, liên tục chấp nhận kết nối từ các máy sinh viên.
public class TcpServer {

    private final int port;
    private final ClientRegistry registry;
    private final Consumer<ClientSession> onClientConnected;

    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private Thread listenerThread;

    public TcpServer(int port, ClientRegistry registry, Consumer<ClientSession> onClientConnected) {
        this.port = port;
        this.registry = registry;
        this.onClientConnected = onClientConnected;
    }

    // Bắt đầu mở cổng lắng nghe trên một luồng Daemon riêng.
    public synchronized void start() {
        if (running) return;

        try {
            serverSocket = new ServerSocket(port);
            running = true;

            listenerThread = new Thread(this::acceptLoop, "TcpServer-Listener");
            listenerThread.setDaemon(true);
            listenerThread.start();

            FileLogger.log("🟢 TcpServer đã khởi động thành công trên cổng " + port);
        } catch (IOException e) {
            FileLogger.log("🔴 Không thể khởi động ServerSocket trên cổng " + port + ": " + e.getMessage());
        }
    }

    private void acceptLoop() {
        while (running && serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                SocketConnection conn = new SocketConnection(clientSocket);

                ClientHandler handler = new ClientHandler(conn, registry, onClientConnected);
                handler.start();
            } catch (IOException e) {
                if (running) {
                    System.err.println("Lỗi khi accept kết nối: " + e.getMessage());
                }
            }
        }
    }

    // Dừng máy chủ an toàn và đóng ServerSocket.
    public synchronized void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}

        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }
}
