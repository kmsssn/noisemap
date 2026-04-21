package kz.noisemap.recordingservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Хранение аудиофайлов. Для MVP — файловая система.
 * В продакшене заменяется на MinIO/S3 без изменения интерфейса.
 */
@Service
public class FileStorageService {

    @Value("${storage.audio.path:/data/audio}")
    private String audioStoragePath;

    public String store(MultipartFile file, UUID userId) throws IOException {
        Path dir = Paths.get(audioStoragePath, userId.toString());
        Files.createDirectories(dir);

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = dir.resolve(filename);
        file.transferTo(filePath.toFile());

        return filePath.toString();
    }

    public void delete(String fileUrl) throws IOException {
        Path path = Paths.get(fileUrl);
        Files.deleteIfExists(path);
    }
}
