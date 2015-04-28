package io.github.rmannibucau.jrunning.jpa;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
@Data
public class RunningCheckPoint {
    @Id
    @GeneratedValue
    @Setter(AccessLevel.NONE)
    private long id;

    private long timestamp;

    @Column(precision = 18)
    private double altitude;

    @Column(precision = 18)
    private double latitude;

    @Column(precision = 18)
    private double longitude;

    @Column(precision = 3)
    private float speed;

    @ManyToOne
    private RunningSession session;
}
