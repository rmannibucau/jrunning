package io.github.rmannibucau.jrunning.heroku;

import org.apache.catalina.realm.JAASRealm;
import org.apache.openejb.core.security.jaas.GroupPrincipal;
import org.apache.openejb.core.security.jaas.UserPrincipal;
import org.apache.tomee.embedded.Configuration;
import org.apache.tomee.embedded.Container;

import java.util.concurrent.CountDownLatch;

import static java.lang.Integer.parseInt;

public class JRunningTomEE {
    public static void main(final String[] args) throws InterruptedException {
        // TODO: use a real realm and not this one which is more or less a mock
        final JAASRealm realm = new JAASRealm();
        realm.setAppName("jrunning");
        realm.setConfigFile("jrunning.jaas");
        realm.setUserClassNames(UserPrincipal.class.getName());
        realm.setRoleClassNames(GroupPrincipal.class.getName());

        try (final Container container = new Container(
                    new Configuration().http(parseInt(System.getenv("PORT"))).setRealm(realm))
                .deployClasspathAsWebApp("/jrunning", null)) {
            new CountDownLatch(1).await();
        }
    }
}
