package com.example.eventsphere.service;

import com.example.eventsphere.enums.FileType;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.io.IOException;

@Service
public class FileService {

    private final Tika tika;

    private final S3Client s3Client;

    private final String bucketName;

    private final String s3Endpoint;

    private final int GLOBAL_MAX_SIZE;

    @Autowired
    FileService(Tika tika, S3Client s3Client, @Value("${supabase.s3.bucket-name}") String bucketName, @Value("${supabase.s3.endpoint}") String s3Endpoint,@Value("${global.max-file-size}") int globalMaxSize){
        this.tika = tika;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.s3Endpoint = s3Endpoint;
        this.GLOBAL_MAX_SIZE = globalMaxSize;
    }
    public void validateFile(MultipartFile file, FileType category) throws IOException {

        if (file.getSize() > ((long) GLOBAL_MAX_SIZE * 1024 * 1024)) {
            throw new RuntimeException("File size exceeds the "+GLOBAL_MAX_SIZE+"MB limit.");
        }

        String detectedType = tika.detect(file.getInputStream());

        if (!category.getMimeTypes().contains(detectedType)) {
            throw new RuntimeException("Invalid file format. Allowed: " + category.getMimeTypes());
        }

        // Optional: Extra check only for images
        if (category == FileType.IMAGE) {
            if (ImageIO.read(file.getInputStream()) == null) {
                throw new RuntimeException("Corrupted image file.");
            }
        }
    }

    public String saveFile(MultipartFile file, FileType category) throws IOException {
        validateFile(file, category);
        String folderName = getFolderName(category);

        // 3. Generate unique filename and full path
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String fullPath = folderName + "/" + fileName;
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fullPath) // <--- Folders are handled by the 'Key'
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        // 5. Construct and Return the Public URL
        // From S3 Endpoint: https://supabase.co
        // To Public URL:    https://project.supabase.co/storage/v1/object/public/event_sphere/images/admin.png
        String projectBaseUrl = s3Endpoint.replace("/v1/s3", "");
        return String.format("%s/v1/object/public/%s/%s", projectBaseUrl, bucketName, fullPath);


    }

    private String getFolderName(FileType category) {
        return switch (category) {
            case IMAGE -> "images";
            case VIDEO -> "videos";
            case DOCUMENT -> "docs";
            default -> "others";
        };
    }

    public void deleteFile(String fileUrl) {
        String fileKey = extractPathFromUrl(fileUrl);

        if (fileKey != null) {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        }
    }

    private String extractPathFromUrl(String url) {
        // Standard format: .../public/bucket_name/folder/file.ext
        String marker = "/public/" + bucketName + "/";
        if (url.contains(marker)) {
            return url.substring(url.indexOf(marker) + marker.length());
        }
        return null;
    }


}
