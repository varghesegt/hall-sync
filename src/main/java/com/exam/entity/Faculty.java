package com.exam.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "faculty")
public class Faculty {

    @Id
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "employee_id", nullable = false, unique = true, length = 50)
    private String employeeId;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(length = 100)
    private String designation;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "college_name", length = 255)
    private String collegeName;

    @Column(name = "is_internal")
    private Boolean isInternal = true;

    @Column(name = "is_available")
    private Boolean isAvailable = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Faculty() {}

    public Faculty(UUID id, String name, String employeeId, String department,
                   String designation, String phone, String email, 
                   String collegeName, Boolean isInternal, Boolean isAvailable) {
        this.id = id;
        this.name = name;
        this.employeeId = employeeId;
        this.department = department;
        this.designation = designation;
        this.phone = phone;
        this.email = email;
        this.isActive = true;
        this.collegeName = collegeName;
        this.isInternal = isInternal != null ? isInternal : true;
        this.isAvailable = isAvailable != null ? isAvailable : true;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmployeeId() { return employeeId; }
    public String getDepartment() { return department; }
    public String getDesignation() { return designation; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public Boolean getIsActive() { return isActive; }
    public String getCollegeName() { return collegeName; }
    public Boolean getIsInternal() { return isInternal; }
    public Boolean getIsAvailable() { return isAvailable; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setName(String name) { this.name = name; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public void setDepartment(String department) { this.department = department; }
    public void setDesignation(String designation) { this.designation = designation; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setEmail(String email) { this.email = email; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public void setCollegeName(String collegeName) { this.collegeName = collegeName; }
    public void setIsInternal(Boolean isInternal) { this.isInternal = isInternal; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }
}
