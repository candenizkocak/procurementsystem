package com.polatholding.procurementsystem.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Departments")
@EqualsAndHashCode(exclude = {"managerUser"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DepartmentID")
    private Integer departmentId;

    @Column(name = "DepartmentName", nullable = false, length = 100)
    private String departmentName;

    @Column(name = "ManagerUserID", insertable=false, updatable=false) // Keep the original column for raw ID access if needed
    private Integer managerUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ManagerUserID", referencedColumnName = "UserID") // This is the actual mapping
    @JsonIgnore
    private User managerUser;
}