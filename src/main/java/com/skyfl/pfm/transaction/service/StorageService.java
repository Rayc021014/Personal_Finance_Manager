package com.skyfl.pfm.transaction.service;

import com.skyfl.pfm.common.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService {

    private final Path uploadDir;

    public StorageService(@Value("${app.storage.upload-dir}") String uploadDir) throws IOException {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    public StoredFile store(MultipartFile file) {
        try {
            String storedName = UUID.randomUUID() + "-" + file.getOriginalFilename();
            Path target = uploadDir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(file.getOriginalFilename(), target.toString(), file.getContentType(), file.getSize());
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store attachment");
        }
    }

    public record StoredFile(String originalName, String absolutePath, String contentType, long size) {
    }
}
