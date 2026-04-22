package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.storage.DataStore;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final DataStore store = DataStore.get();
    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public List<SensorReading> history() {
        return store.readingsFor(sensorId);
    }

    @POST
    public Response append(SensorReading incoming) {
        Sensor parent = store.findSensor(sensorId);
        if (parent == null) {
            // Shouldn't really hit this, the locator already checks, but keeps
            // the resource safe if someone instantiates it directly.
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        if ("MAINTENANCE".equalsIgnoreCase(parent.getStatus())
                || "OFFLINE".equalsIgnoreCase(parent.getStatus())) {
            throw new SensorUnavailableException(sensorId, parent.getStatus());
        }

        if (incoming == null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\":\"BadRequest\",\"message\":\"Reading body is required.\"}")
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }

        if (incoming.getId() == null || incoming.getId().isBlank()) {
            incoming.setId(UUID.randomUUID().toString());
        }
        if (incoming.getTimestamp() <= 0) {
            incoming.setTimestamp(System.currentTimeMillis());
        }

        store.appendReading(sensorId, incoming);
        parent.setCurrentValue(incoming.getValue());

        return Response.status(Response.Status.CREATED).entity(incoming).build();
    }
}
