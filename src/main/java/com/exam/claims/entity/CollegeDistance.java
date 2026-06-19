package com.exam.claims.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(name = "college_distances")
public class CollegeDistance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "institution_code", length = 10)
    private String institutionCode;

    @NotBlank(message = "Institution name is required")
    @Column(name = "institution_name", nullable = false, length = 500)
    private String institutionName;

    @Column(name = "place", length = 200)
    private String place;

    @NotNull(message = "Distance in KM is required")
    @DecimalMin(value = "0.0", message = "Distance must be 0 or greater")
    @Column(name = "distance_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceKm;

    // ========== Constructors ==========

    public CollegeDistance() {}

    public CollegeDistance(String institutionCode, String institutionName, String place, BigDecimal distanceKm) {
        this.institutionCode = institutionCode;
        this.institutionName = institutionName;
        this.place = place;
        this.distanceKm = distanceKm;
    }

    // ========== Getters and Setters ==========

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInstitutionCode() { return institutionCode; }
    public void setInstitutionCode(String institutionCode) { this.institutionCode = institutionCode; }

    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }

    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }

    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }
}
