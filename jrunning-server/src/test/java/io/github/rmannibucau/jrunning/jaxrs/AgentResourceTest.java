package io.github.rmannibucau.jrunning.jaxrs;

import io.github.rmannibucau.jrunning.jaxrs.test.Tx;
import io.github.rmannibucau.jrunning.jpa.RunningCheckPoint;
import io.github.rmannibucau.jrunning.jpa.RunningSession;
import org.apache.openejb.api.configuration.PersistenceUnitDefinition;
import org.apache.openejb.junit.ApplicationComposer;
import org.apache.openejb.testing.Classes;
import org.apache.openejb.testing.ContainerProperties;
import org.apache.openejb.testing.Default;
import org.apache.openejb.testing.EnableServices;
import org.apache.openejb.testing.RandomPort;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import static java.util.stream.Collectors.toList;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@EnableServices("jaxrs")
@PersistenceUnitDefinition
@Default
@ContainerProperties(@ContainerProperties.Property(name = "cxf-rs.auth", value = "BASIC"))
@Classes(cdi = true, context = "jrunning")
@RunWith(ApplicationComposer.class)
public class AgentResourceTest {
    @PersistenceContext
    private EntityManager em;

    @RandomPort("http")
    private URL root;

    @Inject
    private Tx tx;

    @Test
    public void createSession() {
        final WebTarget target = ClientBuilder.newClient().target(root.toExternalForm() + "jrunning");
        final long id = target.path("api/agent/start").request().header("Authorization", "Basic " + printBase64Binary("jonathan:secret".getBytes())).get(Long.class);

        final RunningSession runningSession = em.find(RunningSession.class, id);
        assertNotNull(runningSession);
        assertEquals("jonathan", runningSession.getUsername());

        tx.run(() -> em.createQuery("delete from RunningSession rs").executeUpdate());
    }

    @Test
    public void createSessionAndRun() {
        final WebTarget target = ClientBuilder.newClient().target(root.toExternalForm() + "jrunning");
        final long id = target.path("api/agent/start").request().header("Authorization", "Basic " + printBase64Binary("jonathan:secret".getBytes())).get(Long.class);

        final RunningSession runningSession = em.find(RunningSession.class, id);
        assertNotNull(runningSession);
        assertEquals("jonathan", runningSession.getUsername());

        final Invocation.Builder pointInvocation = target.path("api/agent/point/{sessionId}")
                .resolveTemplate("sessionId", runningSession.getId())
                .request()
                .header("Authorization", "Basic " + printBase64Binary("jonathan:secret".getBytes()));
        final Collection<Point> expected = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            final Point point = new Point();
            point.setLatitude(i);
            point.setAltitude(i * 2);
            point.setLongitude(i * 3);
            point.setSpeed(i * 5);
            expected.add(point);
            pointInvocation.post(Entity.entity(point, MediaType.APPLICATION_JSON_TYPE));
        }

        final Consumer<Collection<RunningCheckPoint>> validateList = (points) -> {
            assertNotNull(points);
            assertEquals(10, points.size());
            assertEquals(expected, points.stream().map(this::toPoint).collect(toList()));
        };
        validateList.accept(tx.run(() -> em.find(RunningSession.class, id).getPoints()));
        validateList.accept(tx.run(() ->
                em.createQuery("select p from RunningCheckPoint p where p.session.id = :id order by p.timestamp", RunningCheckPoint.class)
                        .setParameter("id", runningSession.getId())
                        .getResultList()));

        tx.run(() -> em.createQuery("delete from RunningSession rs").executeUpdate());
        tx.run(() -> em.createQuery("delete from RunningCheckPoint rs").executeUpdate());
    }

    private Point toPoint(final RunningCheckPoint checkPoint) {
        final Point point = new Point();
        point.setLatitude(checkPoint.getLatitude());
        point.setAltitude(checkPoint.getAltitude());
        point.setLongitude(checkPoint.getLongitude());
        point.setSpeed(checkPoint.getSpeed());
        return point;
    }
}
