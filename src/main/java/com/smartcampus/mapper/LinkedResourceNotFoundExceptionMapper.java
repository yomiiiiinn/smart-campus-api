package com.smartcampus.mapper;

import com.smartcampus.exception.LinkedResourceNotFoundException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    private static final int UNPROCESSABLE_ENTITY = 422;

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "LinkedResourceNotFound");
        body.put("status", UNPROCESSABLE_ENTITY);
        body.put("resourceType", ex.getResourceType());
        body.put("referencedId", ex.getReferencedId());
        body.put("message", ex.getMessage());

        return Response.status(UNPROCESSABLE_ENTITY)
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
