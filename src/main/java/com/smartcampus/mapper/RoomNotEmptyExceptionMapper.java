package com.smartcampus.mapper;

import com.smartcampus.exception.RoomNotEmptyException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "RoomNotEmpty");
        body.put("status", 409);
        body.put("roomId", ex.getRoomId());
        body.put("sensorCount", ex.getSensorCount());
        body.put("message", ex.getMessage());
        body.put("hint", "Remove or reassign the sensors before deleting this room.");

        return Response.status(Response.Status.CONFLICT)
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
