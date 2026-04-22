package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;
import com.smartcampus.storage.DataStore;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Context;

import java.util.List;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.get();

    @Context
    private UriInfo uriInfo;

    @GET
    public List<Sensor> listSensors(@QueryParam("type") String type) {
        List<Sensor> all = store.allSensors();
        if (type == null || type.isBlank()) {
            return all;
        }
        return all.stream()
                .filter(s -> type.equalsIgnoreCase(s.getType()))
                .collect(Collectors.toList());
    }

    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\":\"BadRequest\",\"message\":\"Sensor id is required.\"}")
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }
        if (sensor.getRoomId() == null || store.findRoom(sensor.getRoomId()) == null) {
            throw new LinkedResourceNotFoundException("Room", String.valueOf(sensor.getRoomId()));
        }
        if (store.findSensor(sensor.getId()) != null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.CONFLICT)
                            .entity("{\"error\":\"DuplicateSensor\",\"message\":\"A sensor with id '" + sensor.getId() + "' already exists.\"}")
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }
        store.addSensor(sensor);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(sensor.getId()).build())
                .entity(sensor)
                .build();
    }

    @GET
    @Path("/{sensorId}")
    public Sensor getSensor(@PathParam("sensorId") String sensorId) {
        Sensor s = store.findSensor(sensorId);
        if (s == null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND)
                            .entity("{\"error\":\"SensorNotFound\",\"message\":\"No sensor with id '" + sensorId + "'.\"}")
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }
        return s;
    }

    /**
     * Sub-resource locator. Delegates everything under
     * /sensors/{sensorId}/readings to SensorReadingResource.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource readings(@PathParam("sensorId") String sensorId) {
        Sensor s = store.findSensor(sensorId);
        if (s == null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND)
                            .entity("{\"error\":\"SensorNotFound\",\"message\":\"No sensor with id '" + sensorId + "'.\"}")
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }
        return new SensorReadingResource(sensorId);
    }
}
