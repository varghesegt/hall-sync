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

    protected Hall() {}

    public Hall(String id, String name, Integer capacity, Integer internalCapacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.internalCapacity = internalCapacity;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Integer getCapacity() { return capacity; }
    public Integer getInternalCapacity() { return internalCapacity; }

    public void setName(String name) { this.name = name; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public void setInternalCapacity(Integer internalCapacity) { this.internalCapacity = internalCapacity; }
}
