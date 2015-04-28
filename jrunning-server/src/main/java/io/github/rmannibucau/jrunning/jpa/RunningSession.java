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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;

import static javax.persistence.TemporalType.DATE;

@Entity
@Getter
@Setter
@NamedQueries({
        @NamedQuery(name = "RunningSession.findByUsername", query = "select s from RunningSession s where s.username = :username order by s.date desc")
})
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
    private List<RunningCheckPoint> points;
}
