package io.github.rmannibucau.jrunning.jaxrs.test;

import java.util.concurrent.Callable;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

@ApplicationScoped
public class Tx { // just a test helper to not worry about tx/catch
    @Transactional
    public <T> T run(Callable<T> run) {
        try {
            return run.call();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
