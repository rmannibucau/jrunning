package io.github.rmannibucau.jrunning.jaxrs;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class SessionDetail {
    private long id;
    private Date date;
    private List<Point> points;
}
