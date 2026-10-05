package com.greenmobility.common.service;

import com.greenmobility.common.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private final Path rootLocation;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final int MAX_IMAGE_DIMENSION = 1920;
    private static final float JPEG_COMPRESSION_QUALITY = 0.80f;

    public FileStorageService(@Value("${storage.upload-dir:./uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Không thể khởi tạo thư mục lưu trữ uploads: " + e.getMessage(), e);
        }
    }

    public String storeFile(MultipartFile file, String subDirectory) {
        return storeFile(file, subDirectory, null);
    }

    public String storeFile(MultipartFile file, String subDirectory, String fileNamePrefix) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Định dạng tệp không được hỗ trợ. Chỉ chấp nhận các định dạng ảnh: JPG, JPEG, PNG, WEBP");
        }

        try {
            Path targetDir = rootLocation.resolve(subDirectory).normalize();
            if (!targetDir.startsWith(rootLocation)) {
                throw new BadRequestException("Đường dẫn lưu trữ không hợp lệ");
            }
            Files.createDirectories(targetDir);

            String filename;
            long timestamp = System.currentTimeMillis();
            if (fileNamePrefix != null && !fileNamePrefix.isBlank()) {
                filename = fileNamePrefix + "_" + timestamp + extension;
            } else {
                filename = UUID.randomUUID().toString() + extension;
            }

            Path targetLocation = targetDir.resolve(filename);

            // Tối ưu nén ảnh trước khi lưu trữ
            boolean compressed = tryCompressAndSaveImage(file, targetLocation);
            if (!compressed) {
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            // Chuẩn hóa path trả về dạng /uploads/{subDirectory}/{filename}
            String normalizedSub = subDirectory.replace("\\", "/");
            if (!normalizedSub.startsWith("/")) {
                normalizedSub = "/" + normalizedSub;
            }
            if (normalizedSub.endsWith("/")) {
                normalizedSub = normalizedSub.substring(0, normalizedSub.length() - 1);
            }

            return "/uploads" + normalizedSub + "/" + filename;
        } catch (IOException ex) {
            throw new RuntimeException("Lỗi lưu trữ tệp tin: " + ex.getMessage(), ex);
        }
    }

    /**
     * Tối ưu hóa nén ảnh 2 tầng:
     * 1. Smart Resize: Giới hạn cạnh lớn nhất về tối đa 1920px (Full HD) với Bilinear/Bicubic Interpolation.
     * 2. Lossy Compression: Nén JPEG với hệ số chất lượng Q = 0.80 (80%).
     * Tiết kiệm 80% - 90% dung lượng lưu trữ so với ảnh gốc từ camera điện thoại.
     */
    private boolean tryCompressAndSaveImage(MultipartFile file, Path targetLocation) {
        try {
            byte[] bytes = file.getBytes();
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(bytes));
            if (original == null) {
                return false;
            }

            int origWidth = original.getWidth();
            int origHeight = original.getHeight();

            int targetWidth = origWidth;
            int targetHeight = origHeight;

            if (origWidth > MAX_IMAGE_DIMENSION || origHeight > MAX_IMAGE_DIMENSION) {
                if (origWidth > origHeight) {
                    targetWidth = MAX_IMAGE_DIMENSION;
                    targetHeight = (int) (((double) origHeight / origWidth) * MAX_IMAGE_DIMENSION);
                } else {
                    targetHeight = MAX_IMAGE_DIMENSION;
                    targetWidth = (int) (((double) origWidth / origHeight) * MAX_IMAGE_DIMENSION);
                }
            }

            BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
            g.dispose();

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                writers = ImageIO.getImageWritersByFormatName("jpeg");
            }

            if (!writers.hasNext()) {
                ImageIO.write(resized, "jpg", targetLocation.toFile());
                return true;
            }

            ImageWriter writer = writers.next();
            try (OutputStream os = new FileOutputStream(targetLocation.toFile());
                 ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(JPEG_COMPRESSION_QUALITY);
                }
                writer.write(null, new IIOImage(resized, null, null), param);
            } finally {
                writer.dispose();
            }

            long originalSize = bytes.length;
            long compressedSize = Files.size(targetLocation);
            double savingPercent = (1.0 - ((double) compressedSize / originalSize)) * 100.0;
            log.info("Tối ưu nén ảnh: {} -> {} bytes (giảm {:.1f}%)", originalSize, compressedSize, savingPercent);
            return true;
        } catch (Exception e) {
            log.warn("Không thể nén ảnh tự động, sử dụng ảnh nguyên bản: {}", e.getMessage());
            return false;
        }
    }
}
