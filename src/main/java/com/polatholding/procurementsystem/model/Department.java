package com.polatholding.procurementsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data // Generates getters, setters, toString(), equals(), and hashCode()
@NoArgsConstructor // Generates a no-argument constructor
@AllArgsConstructor // Generates a constructor with all arguments
@Entity
@Table(name = "Departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DepartmentID")
    private Integer departmentId;

    @Column(name = "DepartmentName", nullable = false, length = 100)
    private String departmentName;

    @Column(name = "ManagerUserID")
    private Integer managerUserId;
}