package io.github.rmannibucau.jrunning.jaxrs;

import io.github.rmannibucau.jrunning.jaxrs.security.Secured;
import io.github.rmannibucau.jrunning.jpa.RunningCheckPoint;
import io.github.rmannibucau.jrunning.jpa.RunningSession;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import static java.util.stream.Collectors.toList;

@Secured
@Path("report")
public class ReportResource {
    @Context
    private SecurityContext securityContext;

    @PersistenceContext
    private EntityManager em;

    @GET
    @Path("sessions")
    public List<SessionSummary> sessions(@QueryParam("first") @DefaultValue("0") final int first,
                                         @QueryParam("max") @DefaultValue("30") final int max) {
        return em.createNamedQuery("RunningSession.findByUsername", RunningSession.class)
                .setFirstResult(first)
                .setMaxResults(max)
                .setParameter("username", securityContext.getUserPrincipal().getName())
                .getResultList()
                .stream()
                .map(this::toSessionSummary)
                .collect(toList());
    }

    @GET
    @Path("session/{id}")
    @Transactional
    public SessionDetail session(@PathParam("id") final String id) {
        final RunningSession entity = Objects.requireNonNull(em.find(RunningSession.class, id));
        final SessionDetail sessionDetail = new SessionDetail();
        sessionDetail.setId(entity.getId());
        sessionDetail.setDate(new Date(entity.getDate().getTime()));
        sessionDetail.setPoints(
                Optional.ofNullable(entity.getPoints()).orElse(Collections.emptyList())
                    .stream()
                    .map(this::toPoint)
                    .collect(toList()));
        return sessionDetail;
    }

    private Point toPoint(final RunningCheckPoint checkPoint) {
        final Point point = new Point();
        point.setLatitude(checkPoint.getLatitude());
        point.setLongitude(checkPoint.getLongitude());
        point.setAltitude(checkPoint.getAltitude());
        point.setTimestamp(checkPoint.getTimestamp());
        point.setSpeed(checkPoint.getSpeed());
        return point;
    }

    private SessionSummary toSessionSummary(final RunningSession runningSession) {
        final SessionSummary sessionSummary = new SessionSummary();
        sessionSummary.setId(runningSession.getId());
        sessionSummary.setDate(new Date(runningSession.getDate().getTime()));
        return sessionSummary;
    }
}
