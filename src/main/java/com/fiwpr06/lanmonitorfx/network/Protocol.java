package com.fiwpr06.lanmonitorfx.network;

import java.util.Arrays;

/**
 * Định nghĩa giao thức truyền thông TCP/IP cho LanMonitorFX.
 * Bao gồm chính xác 9 mã lệnh cốt lõi của đề tài học thuật:
 * LOGIN, SCREEN_DATA, LOCK, UNLOCK, SHUTDOWN, RESTART, LOGOUT, NOTIFY, DISCONNECT.
 * Toàn bộ logic đóng gói (build) và giải mã (parse) chuỗi gói tin tập trung tại đây.
 */
public final class Protocol {

    // --- CẤU HÌNH MẠNG ---
    public static final int PORT = 9999;
    public static final String SEPARATOR = "###";

    // --- 9 LỆNH CỐT LÕI ---
    public static final String CMD_LOGIN        = "LOGIN";
    public static final String CMD_SCREEN_DATA  = "SCREEN_DATA";
    public static final String CMD_LOCK         = "LOCK";
    public static final String CMD_UNLOCK       = "UNLOCK";
    public static final String CMD_SHUTDOWN     = "SHUTDOWN";
    public static final String CMD_RESTART      = "RESTART";
    public static final String CMD_LOGOUT       = "LOGOUT";
    public static final String CMD_NOTIFY       = "NOTIFY";
    public static final String CMD_DISCONNECT   = "DISCONNECT";

    private Protocol() {}

    /**
     * Phân tích chuỗi nhận được từ socket thành đối tượng Message.
     * Định dạng: CMD###param0###param1...
     */
    public static Message parse(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return new Message("", new String[0]);
        }

        String[] parts = rawMessage.split(SEPARATOR, -1);
        String command = parts[0].trim();

        // Xử lý đặc biệt cho NOTIFY: bảo toàn toàn bộ nội dung tin nhắn nếu chứa ký tự phân cách
        if (CMD_NOTIFY.equalsIgnoreCase(command) && parts.length > 2) {
            String fullMessage = rawMessage.substring(command.length() + SEPARATOR.length());
            return new Message(command, new String[]{fullMessage});
        }

        String[] params = parts.length > 1
                ? Arrays.copyOfRange(parts, 1, parts.length)
                : new String[0];

        return new Message(command, params);
    }

    // Đóng gói lệnh tổng quát kèm danh sách tham số: CMD###param1###param2...
    public static String build(String command, String... params) {
        if (params == null || params.length == 0) {
            return command;
        }
        StringBuilder sb = new StringBuilder(command);
        for (String param : params) {
            sb.append(SEPARATOR).append(param != null ? param : "");
        }
        return sb.toString();
    }

    // Tạo gói tin LOGIN: LOGIN###mssv###hoten
    public static String buildLogin(String mssv, String hoTen) {
        return build(CMD_LOGIN, mssv, hoTen);
    }

    // Tạo gói tin SCREEN_DATA: SCREEN_DATA###clientId###base64Image
    public static String buildScreenData(String clientId, String base64Image) {
        return build(CMD_SCREEN_DATA, clientId, base64Image);
    }

    // Tạo gói tin NOTIFY chung: NOTIFY###message
    public static String buildNotify(String message) {
        return build(CMD_NOTIFY, message);
    }

    // Tạo gói tin NOTIFY gửi tới CẢ LỚP: NOTIFY###[CẢ LỚP] message
    public static String buildBroadcastNotify(String message) {
        return build(CMD_NOTIFY, "[CẢ LỚP] " + message);
    }

    // Tạo gói tin NOTIFY gửi RIÊNG cho 1 học sinh: NOTIFY###[GỬI RIÊNG] message
    public static String buildPrivateNotify(String message) {
        return build(CMD_NOTIFY, "[GỬI RIÊNG] " + message);
    }

    // Tạo gói tin NOTIFY gửi RIÊNG cho một nhóm máy tính: NOTIFY###[GỬI RIÊNG NHÓM N MÁY] message
    public static String buildGroupNotify(int count, String message) {
        return build(CMD_NOTIFY, "[GỬI RIÊNG NHÓM " + count + " MÁY] " + message);
    }
}
