package com.fiwpr06.lanmonitorfx.client;

import com.fiwpr06.lanmonitorfx.util.ImageUtil;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Dịch vụ chụp ảnh màn hình máy sinh viên định kỳ và gửi về Server.
 * Sử dụng ScheduledExecutorService chạy ngầm chu kỳ 2 giây/lần.
 * Ảnh chụp được thu nhỏ (thumbnail) và nén JPG chất lượng 0.65f để tiết kiệm băng thông mạng.
 */
public class ScreenCaptureService {

    private static final int PERIOD_SECONDS = 2;
    private static final int TARGET_WIDTH = 480;
    private static final int TARGET_HEIGHT = 270;
    private static final float JPEG_QUALITY = 0.65f;

    private final TcpClient tcpClient;
    private ScheduledExecutorService scheduler;
    private Robot robot;
    private Rectangle screenRect;
    private final AtomicLong frameCount = new AtomicLong(0);

    public ScreenCaptureService(TcpClient tcpClient) {
        this.tcpClient = tcpClient;
        try {
            this.robot = new Robot();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            this.screenRect = new Rectangle(screenSize);
        } catch (Exception e) {
            System.err.println("Không thể khởi tạo Robot chụp màn hình: " + e.getMessage());
        }
    }

    // Bắt đầu chu kỳ chụp màn hình định kỳ 2 giây/lần.
    public synchronized void start() {
        if (scheduler != null && !scheduler.isShutdown()) return;

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ScreenCapture-Thread");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(this::captureAndSend, 0, PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    private void captureAndSend() {
        if (robot == null || screenRect == null || !tcpClient.isConnected()) {
            return;
        }

        try {
            // 1. Chụp ảnh màn hình thực tế
            BufferedImage fullScreen = robot.createScreenCapture(screenRect);

            // 2. Thu nhỏ về kích thước thumbnail tối ưu truyền mạng
            BufferedImage thumbnail = ImageUtil.resize(fullScreen, TARGET_WIDTH, TARGET_HEIGHT);

            // 3. Nén sang định dạng Base64 JPEG
            String base64 = ImageUtil.toBase64Jpeg(thumbnail, JPEG_QUALITY);

            // 4. Gửi chuỗi ảnh qua socket
            if (base64 != null && !base64.isBlank()) {
                tcpClient.sendScreenData(base64);
                frameCount.incrementAndGet();
            }
        } catch (Exception e) {
            System.err.println("Lỗi trong chu kỳ chụp và gửi ảnh: " + e.getMessage());
        }
    }

    public long getFrameCount() {
        return frameCount.get();
    }

    // Dừng chu kỳ chụp màn hình và giải phóng luồng lập lịch.
    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}
