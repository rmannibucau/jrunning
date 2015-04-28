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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@EnableServices("jaxrs")
@PersistenceUnitDefinition
@Default
@ContainerProperties({
    @ContainerProperties.Property(name = "cxf-rs.auth", value = "BASIC")
})
@Classes(cdi = true, context = "jrunning")
@RunWith(ApplicationComposer.class)
public class ReportResourceTest {
    @RandomPort("http")
    private URL root;

    @Inject
    private Tx tx;

    @PersistenceContext
    private EntityManager em;

    private final AtomicLong lastId = new AtomicLong();

    @Before
    public void initData() {
        tx.run(() -> {
            for (int i = 0; i < 5; i++) {
                final RunningSession session = new RunningSession();
                session.setDate(new Date());
                session.setUsername("jonathan");
                em.persist(session);

                for (int j = 0; j < 10; j++) {
                    final RunningCheckPoint point = new RunningCheckPoint();
                    point.setSession(session);
                    point.setSpeed(i + 10.1f);
                    point.setLatitude(i * 100);
                    point.setLongitude(i * 200);
                    point.setAltitude(i * 300);
                    point.setTimestamp(session.getDate().getTime() + i * 500);
                    em.persist(point);
                }

                em.flush();
                lastId.set(session.getId());
            }

            final RunningSession session = new RunningSession();
            session.setDate(new Date());
            session.setUsername("other");
            em.persist(session);
            return null;
        });
    }

    @Test
    public void sessions() {
        final List<SessionSummary> sessions = ClientBuilder.newClient()
                .target(root.toExternalForm() + "jrunning/api")
                .path("report/sessions")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Basic " + printBase64Binary("jonathan:secret".getBytes()))
                .get(new GenericType<List<SessionSummary>>() {});
        assertNotNull(sessions);
        assertEquals(5, sessions.size());
    }

    @Test
    public void session() {
        final SessionDetail session = ClientBuilder.newClient()
                .target(root.toExternalForm() + "jrunning/api")
                .path("report/session/{id}").resolveTemplate("id", lastId.longValue())
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Basic " + printBase64Binary("jonathan:secret".getBytes()))
                .get(SessionDetail.class);
        assertNotNull(session);
        assertNotNull(session.getDate());
        assertTrue(session.getId() > 0);
        assertEquals(10, session.getPoints().size());
        session.getPoints().stream().forEach(p -> assertTrue(p.getTimestamp() > session.getDate().getTime()));
    }
}
