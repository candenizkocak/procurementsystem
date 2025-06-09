package com.polatholding.procurementsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "RequestHistory")
public class RequestHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HistoryID")
    private Integer historyId;

    @Column(name = "RequestID", nullable = false)
    private Integer requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", referencedColumnName = "UserID")
    private User user;

    @Column(name = "Action", nullable = false, length = 100)
    private String action;

    @Column(name = "Details", length = 500)
    private String details;

    @Column(name = "EventDate", nullable = false)
    private LocalDateTime eventDate;
}