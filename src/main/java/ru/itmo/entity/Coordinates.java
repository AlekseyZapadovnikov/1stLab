package ru.itmo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "coordinates")
public class Coordinates {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "x", nullable = false)
    private Long x;

    @Column(name = "y", nullable = false)
    private Float y;

    public int getId() {
        return id;
    }

    public Long getX() {
        return x;
    }

    public void setX(Long x) {
        this.x = x;
    }

    public Float getY() {
        return y;
    }

    public void setY(Float y) {
        this.y = y;
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        if (x == null) {
            throw new IllegalArgumentException("Координата x обязательна");
        }
        if (y == null) {
            throw new IllegalArgumentException("Координата y обязательна");
        }
        if (!(y <= 791)) {
            throw new IllegalArgumentException("Координата y должна быть не больше 791");
        }
    }
}
