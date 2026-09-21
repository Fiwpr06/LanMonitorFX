package com.fiwpr06.lanmonitorfx.network;

/**
 * Model đại diện cho một thông điệp giao thức đã được phân tích cú pháp.
 * Định dạng: command###param0###param1...
 *
 * @param command Tên lệnh (ví dụ: LOGIN, SCREEN_DATA, LOCK...)
 * @param params  Mảng tham số đi kèm
 */
public record Message(String command, String[] params) {

    public Message(String command) {
        this(command, new String[0]);
    }

    /**
     * Lấy tham số tại chỉ mục i, nếu không tồn tại trả về chuỗi rỗng.
     *
     * @param index Chỉ mục tham số (0-based)
     * @return Giá trị tham số hoặc "" nếu out of bounds
     */
    public String param(int index) {
        if (params != null && index >= 0 && index < params.length) {
            return params[index];
        }
        return "";
    }

    // Số lượng tham số của thông điệp.
    public int paramCount() {
        return params != null ? params.length : 0;
    }
}
