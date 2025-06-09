package com.polatholding.procurementsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Files")
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FileID")
    private Integer fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RequestID", nullable = false)
    private PurchaseRequest purchaseRequest;

    @Column(name = "FilePath", nullable = false, length = 200)
    private String filePath;

    @Column(name = "FileType", length = 50)
    private String fileType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UploadedByUserID", nullable = false)
    private User uploadedByUser;

    @Column(name = "UploadedAt", nullable = false)
    private LocalDateTime uploadedAt;
}