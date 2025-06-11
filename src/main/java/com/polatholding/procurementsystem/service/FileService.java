package com.polatholding.procurementsystem.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    void uploadFiles(Integer requestId, List<MultipartFile> multipartFiles, String username);

    Resource loadAsResource(Integer fileId);
}
