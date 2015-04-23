package io.github.rmannibucau.jrunning.jpa;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

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
    private double altitude;
    private double latitude;
    private double longitude;
    private float speed;

    @ManyToOne
    private RunningSession session;
}
