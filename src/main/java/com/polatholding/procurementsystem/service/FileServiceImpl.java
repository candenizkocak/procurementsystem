package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.model.File;
import com.polatholding.procurementsystem.model.PurchaseRequest;
import com.polatholding.procurementsystem.model.User;
import com.polatholding.procurementsystem.repository.FileRepository;
import com.polatholding.procurementsystem.repository.PurchaseRequestRepository;
import com.polatholding.procurementsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final UserRepository userRepository;

    @Value("${file.storage.directory}")
    private String storageDir;

    public FileServiceImpl(FileRepository fileRepository,
                           PurchaseRequestRepository purchaseRequestRepository,
                           UserRepository userRepository) {
        this.fileRepository = fileRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void uploadFiles(Integer requestId, List<MultipartFile> multipartFiles, String username) {
        if (multipartFiles == null || multipartFiles.isEmpty()) {
            return;
        }

        PurchaseRequest request = purchaseRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        for (MultipartFile multipartFile : multipartFiles) {
            if (multipartFile.isEmpty()) continue;
            try {
                java.nio.file.Path dirPath = Path.of(storageDir, String.valueOf(requestId));
                Files.createDirectories(dirPath);
                String filename = System.currentTimeMillis() + "_" + multipartFile.getOriginalFilename();
                Path target = dirPath.resolve(filename);
                Files.copy(multipartFile.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

                File file = new File();
                file.setPurchaseRequest(request);
                file.setFilePath(target.toString());
                file.setFileType(multipartFile.getContentType());
                file.setUploadedByUser(user);
                file.setUploadedAt(LocalDateTime.now());

                fileRepository.save(file);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to store file", ex);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadAsResource(Integer fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
        try {
            Path path = Path.of(file.getFilePath());
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists()) {
                return resource;
            }
            throw new RuntimeException("File not found: " + fileId);
        } catch (IOException e) {
            throw new RuntimeException("File not readable: " + fileId, e);
        }
    }
}
