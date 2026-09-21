package com.fiwpr06.lanmonitorfx.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Tiện ích ghi nhật ký hoạt động mạng LAN theo ngày vào thư mục logs/.
 * Tên file định dạng: logs/NhatKy_dd_MM_yyyy.txt
 * - Hỗ trợ chuẩn UTF-8 toàn diện, chống lỗi dấu '?' trên console Windows và file.
 * - Hỗ trợ đăng ký Listener (Observer Pattern) để giao diện UI có thể cập nhật Live Log theo thời gian thực.
 */
public final class FileLogger {

    private static final String LOG_DIR = "logs";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd_MM_yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    static {
        try {
            // Ép stdout và stderr của JVM sang UTF-8 để hiển thị chính xác tiếng Việt có dấu
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }

    private FileLogger() {}

    /**
     * Đăng ký một listener để nhận các dòng log mới theo thời gian thực (dùng cho Tab Nhật Ký trên giao diện).
     */
    public static void addListener(Consumer<String> listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Hủy đăng ký listener.
     */
    public static void removeListener(Consumer<String> listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Ghi một dòng log với định dạng: [HH:mm:ss] message
     */
    public static synchronized void log(String message) {
        String timestamp = LocalTime.now().format(TIME_FORMAT);
        String logLine = "[" + timestamp + "] " + message;

        // 1. In ra console
        System.out.println(logLine);

        // 2. Ghi nối vào file theo ngày
        writeToFile(logLine);

        // 3. Thông báo tới các listener UI đang lắng nghe
        for (Consumer<String> listener : listeners) {
            try {
                listener.accept(logLine + "\n");
            } catch (Exception ignored) {}
        }
    }

    private static void writeToFile(String logLine) {
        try {
            File dir = new File(LOG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String dateStr = LocalDate.now().format(DATE_FORMAT);
            File logFile = new File(dir, "NhatKy_" + dateStr + ".txt");

            try (var writer = new OutputStreamWriter(new FileOutputStream(logFile, true), StandardCharsets.UTF_8)) {
                writer.write(logLine);
                writer.write(System.lineSeparator());
            }
        } catch (Exception e) {
            System.err.println("Lỗi ghi file nhật ký: " + e.getMessage());
        }
    }

    /**
     * Đọc toàn bộ nội dung file log của ngày hôm nay.
     */
    public static String readTodayLog() {
        try {
            String dateStr = LocalDate.now().format(DATE_FORMAT);
            File logFile = new File(LOG_DIR, "NhatKy_" + dateStr + ".txt");
            if (!logFile.exists()) {
                return "(Chưa có nhật ký hoạt động nào trong ngày hôm nay)\n";
            }
            return java.nio.file.Files.readString(logFile.toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "Lỗi đọc nhật ký: " + e.getMessage();
        }
    }

    /**
     * Lấy đường dẫn file log hôm nay.
     */
    public static File getTodayLogFile() {
        String dateStr = LocalDate.now().format(DATE_FORMAT);
        return new File(LOG_DIR, "NhatKy_" + dateStr + ".txt");
    }

    /**
     * Xóa sạch nội dung file log của ngày hôm nay.
     */
    public static synchronized boolean clearTodayLog() {
        try {
            File logFile = getTodayLogFile();
            if (logFile.exists()) {
                new FileOutputStream(logFile).close(); // Ghi đè rỗng
            }
            log("🧹 Nhật ký hoạt động ngày hôm nay đã được xóa sạch.");
            return true;
        } catch (Exception e) {
            System.err.println("Lỗi xóa file nhật ký: " + e.getMessage());
            return false;
        }
    }
}
