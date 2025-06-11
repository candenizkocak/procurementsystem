package com.polatholding.procurementsystem.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    void uploadFile(Integer requestId, MultipartFile multipartFile, String username);
}
