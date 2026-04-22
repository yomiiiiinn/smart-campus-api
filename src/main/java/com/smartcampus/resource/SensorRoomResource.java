package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.storage.DataStore;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Context;

import java.util.List;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorRoomResource {

    private final DataStore store = DataStore.get();

    @Context
    private UriInfo uriInfo;

    @GET
    public List<Room> listRooms() {
        return store.allRooms();
    }

    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\":\"BadRequest\",\"message\":\"Room id is required.\"}")
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }
        if (store.findRoom(room.getId()) != null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.CONFLICT)
                            .entity("{\"error\":\"DuplicateRoom\",\"message\":\"A room with id '" + room.getId() + "' already exists.\"}")
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }
        store.addRoom(room);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(room.getId()).build())
                .entity(room)
                .build();
    }

    @GET
    @Path("/{roomId}")
    public Room getRoom(@PathParam("roomId") String roomId) {
        Room room = store.findRoom(roomId);
        if (room == null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND)
                            .entity("{\"error\":\"RoomNotFound\",\"message\":\"No room with id '" + roomId + "'.\"}")
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }
        return room;
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.findRoom(roomId);
        if (room == null) {
            // Idempotent path: deleting a missing room returns 204, same as
            // deleting one that existed a moment ago.
            return Response.noContent().build();
        }
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());
        }
        store.removeRoom(roomId);
        return Response.noContent().build();
    }
}
