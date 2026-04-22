package com.smartcampus.mapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Catch-all mapper. Keeps any uncaught RuntimeException from leaking a stack
 * trace to the client. WebApplicationException falls through so Jersey can
 * render its own 404s, 415s, method-not-allowed responses, and so on.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        if (ex instanceof WebApplicationException) {
            return ((WebApplicationException) ex).getResponse();
        }

        String traceId = UUID.randomUUID().toString();
        LOG.log(Level.SEVERE, "[" + traceId + "] unhandled error", ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "InternalServerError");
        body.put("status", 500);
        body.put("traceId", traceId);
        body.put("message", "An unexpected error occurred. Contact the admin with the trace id.");

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
