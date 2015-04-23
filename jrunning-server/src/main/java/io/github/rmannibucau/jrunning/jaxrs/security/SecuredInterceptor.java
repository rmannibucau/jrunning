package io.github.rmannibucau.jrunning.jaxrs.security;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Interceptor
@Secured
@Priority(Interceptor.Priority.APPLICATION)
public class SecuredInterceptor {
    @Context
    private SecurityContext securityContext;

    @AroundInvoke
    public Object check(final InvocationContext ic) throws Exception {
        if (securityContext.getUserPrincipal() == null) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        return ic.proceed();
    }
}
