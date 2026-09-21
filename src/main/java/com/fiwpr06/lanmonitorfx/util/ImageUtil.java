package com.fiwpr06.lanmonitorfx.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Tiện ích xử lý nén ảnh và chuyển đổi định dạng Base64 hai chiều.
 * Hỗ trợ nén ảnh chụp màn hình sang JPG chất lượng tùy chỉnh và chuyển đổi an toàn sang JavaFX Image.
 */
public final class ImageUtil {

    private ImageUtil() {}

    /**
     * Nén BufferedImage thành chuỗi Base64 định dạng JPEG với chất lượng xác định.
     */
    public static String toBase64Jpeg(BufferedImage image, float quality) {
        if (image == null) return "";
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);

            try (var ios = new MemoryCacheImageOutputStream(baos)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            } finally {
                writer.dispose();
            }

            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            System.err.println("Lỗi nén ảnh sang Base64: " + e.getMessage());
            return "";
        }
    }

    /**
     * Thu nhỏ kích thước ảnh giữ tỉ lệ gốc để tối ưu băng thông mạng LAN.
     */
    public static BufferedImage resize(BufferedImage original, int targetWidth, int targetHeight) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return resized;
    }

    /**
     * Chuyển đổi chuỗi Base64 thành JavaFX Image an toàn trên nền tảng luồng khác nhau.
     */
    public static Image fromBase64(String base64) {
        if (base64 == null || base64.isBlank()) return null;

        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return new Image(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            System.err.println("Lỗi giải mã Base64 sang JavaFX Image: " + e.getMessage());
            return null;
        }
    }
}
