package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Context;

import java.util.LinkedHashMap;
import java.util.Map;

@Path("/")
public class DiscoveryResource {

    @Context
    private UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response discovery() {
        String base = uriInfo.getBaseUri().toString();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms", base + "/rooms");
        resources.put("sensors", base + "/sensors");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Smart Campus API");
        body.put("version", "1.0.0");
        body.put("description", "Sensor and room management service for the university campus.");
        body.put("admin", Map.of(
                "name", "Yomin Panwala",
                "studentId", "W1970466",
                "email", "facilities@westminster.example"
        ));
        body.put("resources", resources);
        body.put("documentation", "See README.md in the project repository.");

        return Response.ok(body).build();
    }
}
