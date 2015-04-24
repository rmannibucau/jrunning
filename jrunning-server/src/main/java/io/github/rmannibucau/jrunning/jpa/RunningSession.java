package io.github.rmannibucau.jrunning.jpa;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;

import static javax.persistence.TemporalType.DATE;

@Entity
@Getter
@Setter
public class RunningSession {
    @Id
    @GeneratedValue
    @Setter(AccessLevel.NONE)
    private long id;

    private String username;

    @Temporal(DATE)
    private Date date;

    @OrderBy("timestamp")
    @OneToMany(mappedBy = "session")
    private List<RunningCheckPoint> points = new LinkedList<>();
}
