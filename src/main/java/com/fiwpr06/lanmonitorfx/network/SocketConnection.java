package com.fiwpr06.lanmonitorfx.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Lớp đóng gói kết nối Socket và luồng I/O nhị phân DataInputStream/DataOutputStream.
 * Toàn bộ việc gửi và nhận dữ liệu qua socket trên cả Server và Client đều đi qua lớp này.
 * Sử dụng cơ chế đóng khung TCP độ dài tiền tố (Length-prefixed framing) 4-byte,
 * giúp truyền tải an toàn chuỗi lệnh và dữ liệu ảnh Base64 dung lượng lớn mà không bị giới hạn 64KB như writeUTF.
 */
public class SocketConnection implements AutoCloseable {

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private volatile boolean closed = false;

    public SocketConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    /**
     * Gửi chuỗi thông điệp qua socket, được đồng bộ hóa để tránh xung đột giữa các luồng.
     *
     * @param message Chuỗi thông điệp cần gửi (định dạng CMD###param1###...)
     * @throws IOException Khi có lỗi đường truyền hoặc socket đã đóng
     */
    public synchronized void send(String message) throws IOException {
        if (closed || socket.isClosed()) {
            throw new IOException("Socket đã đóng, không thể gửi dữ liệu.");
        }
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
        out.flush();
    }

    /**
     * Đọc chuỗi thông điệp tiếp theo từ socket (blocking call).
     *
     * @return Chuỗi thông điệp nhận được
     * @throws IOException Khi có lỗi đường truyền hoặc socket bị ngắt kết nối
     */
    public String receive() throws IOException {
        if (closed || socket.isClosed()) {
            throw new IOException("Socket đã đóng, không thể đọc dữ liệu.");
        }
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // Lấy địa chỉ IP từ xa của kết nối.
    public String getRemoteAddress() {
        if (socket.getInetAddress() != null) {
            return socket.getInetAddress().getHostAddress();
        }
        return "Unknown";
    }

    // Kiểm tra socket còn kết nối và chưa đóng hay không.
    public boolean isConnected() {
        return !closed && socket.isConnected() && !socket.isClosed();
    }

    // Đóng an toàn kết nối socket và các luồng liên quan.
    @Override
    public void close() {
        closed = true;
        try {
            if (in != null) in.close();
        } catch (Exception ignored) {}

        try {
            if (out != null) out.close();
        } catch (Exception ignored) {}

        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {}
    }
}
