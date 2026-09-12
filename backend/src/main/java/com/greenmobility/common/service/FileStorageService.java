package com.greenmobility.common.service;

import com.greenmobility.common.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path rootLocation;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");

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
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

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
}
