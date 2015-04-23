package io.github.rmannibucau.jrunning.jaxrs;

import io.github.rmannibucau.jrunning.jaxrs.security.Secured;
import io.github.rmannibucau.jrunning.jpa.RunningCheckPoint;
import io.github.rmannibucau.jrunning.jpa.RunningSession;

import java.util.Objects;
import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

@Secured
@Transactional
@Path("agent")
@ApplicationScoped
public class AgentResource {
    @Context
    private SecurityContext securityContext;

    @PersistenceContext
    private EntityManager em;

    @POST
    @Path("point/{sessionId}")
    public void addPoint(final Point point, @PathParam("sessionId") final long sessionId) {
        final RunningSession session = Objects.requireNonNull(em.find(RunningSession.class, sessionId));
        final RunningCheckPoint checkPoint = new RunningCheckPoint();
        checkPoint.setAltitude(point.getAltitude());
        checkPoint.setLatitude(point.getLatitude());
        checkPoint.setLongitude(point.getLongitude());
        checkPoint.setSpeed(point.getSpeed());
        checkPoint.setTimestamp(point.getTimestamp());
        checkPoint.setSession(session);
        em.persist(checkPoint);
        em.flush();
    }

    @GET
    @Path("start")
    public long newSession() {
        final RunningSession session = new RunningSession();
        session.setUsername(securityContext.getUserPrincipal().getName());
        em.persist(session);
        em.flush();
        return session.getId();
    }
}
