package com.fiwpr06.lanmonitorfx.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Model đại diện cho một thông báo từ Giáo viên gửi tới Sinh viên.
 * Dùng để hiển thị trong Hộp thư thông báo (Notification Feed) phong cách AnyDesk trên giao diện Client.
 *
 * @param time        Thời gian nhận (HH:mm:ss)
 * @param sender      Người gửi ("Giáo viên" hoặc "Hệ thống")
 * @param content     Nội dung thông báo
 * @param isBroadcast true nếu gửi cả lớp, false nếu gửi riêng
 */
public record NotificationItem(String time, String sender, String content, boolean isBroadcast) {

    public static NotificationItem now(String sender, String content, boolean isBroadcast) {
        String t = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        return new NotificationItem(t, sender, content, isBroadcast);
    }
}
