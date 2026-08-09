package com.exam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "halls")
public class Hall {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "internal_capacity", nullable = false)
    private Integer internalCapacity;

    @Column(name = "sem_rows", nullable = false, columnDefinition = "integer default 5")
    private Integer semRows = 5;

    @Column(name = "sem_cols", nullable = false, columnDefinition = "integer default 5")
    private Integer semCols = 5;

    @Column(name = "internal_rows", nullable = false, columnDefinition = "integer default 6")
    private Integer internalRows = 6;

    @Column(name = "internal_cols", nullable = false, columnDefinition = "integer default 8")
    private Integer internalCols = 8;

    protected Hall() {}

    public Hall(String id, String name, Integer capacity, Integer internalCapacity, Integer semRows, Integer semCols, Integer internalRows, Integer internalCols) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.internalCapacity = internalCapacity;
        this.semRows = semRows != null ? semRows : 5;
        this.semCols = semCols != null ? semCols : 5;
        this.internalRows = internalRows != null ? internalRows : 6;
        this.internalCols = internalCols != null ? internalCols : 8;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Integer getCapacity() { return capacity; }
    public Integer getInternalCapacity() { return internalCapacity; }
    public Integer getSemRows() { return semRows; }
    public Integer getSemCols() { return semCols; }
    public Integer getInternalRows() { return internalRows; }
    public Integer getInternalCols() { return internalCols; }

    public void setName(String name) { this.name = name; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public void setInternalCapacity(Integer internalCapacity) { this.internalCapacity = internalCapacity; }
    public void setSemRows(Integer semRows) { this.semRows = semRows; }
    public void setSemCols(Integer semCols) { this.semCols = semCols; }
    public void setInternalRows(Integer internalRows) { this.internalRows = internalRows; }
    public void setInternalCols(Integer internalCols) { this.internalCols = internalCols; }
}
