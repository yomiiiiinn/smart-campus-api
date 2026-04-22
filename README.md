# Smart Campus Sensor and Room Management API

**Module:** 5COSC022W Client-Server Architectures (2025/26)
**Author:** Yomin Panwala
**Student ID:** W1970466
**Module Leader:** Hamed Hamzeh

A JAX-RS RESTful web service that manages Rooms and Sensors across the University of Westminster "Smart Campus" infrastructure. Built with Jersey 2.39 on an embedded Grizzly server, Jackson for JSON, and in-memory thread-safe storage.

---

## 1. API Overview

The service exposes three resource types under a single versioned entry point `/api/v1`. Rooms contain Sensors. Each Sensor keeps a rolling log of SensorReadings reached through a nested sub-resource. Business rules stop you from deleting a room that still has hardware in it, from registering a sensor against a room that does not exist, and from pushing readings into a sensor flagged as MAINTENANCE.

Every error path goes through a dedicated ExceptionMapper so the client never sees a raw stack trace. HTTP status codes follow standard conventions (201 for creation, 204 for idempotent deletes, 409 for conflicts, 422 for invalid references, 403 for forbidden state, 500 for anything else).

---

## 2. Prerequisites

- Java 11 or later (tested on Temurin 11.0.24)
- Maven 3.6 or later
- A tool for hitting HTTP endpoints (curl, Postman, HTTPie)

---

## 3. Build and Run

Clone the repo and change into the project folder:

```bash
git clone https://github.com/yomiiiiinn/smart-campus-api.git
cd smart-campus-api
```

Build a runnable fat jar:

```bash
mvn clean package -DskipTests
```

Start the server:

```bash
java -jar target/smart-campus-api.jar
```

You should see:

```
Smart Campus API running at http://localhost:8080/api/v1/
Press Ctrl+C to stop.
```

Alternatively you can run via the Maven exec plugin without packaging:

```bash
mvn compile exec:java
```

The API is now reachable at `http://localhost:8080/api/v1`.

---

## 4. Endpoint Reference

| Method | Path                                    | Purpose                                           | Success | Failure codes |
| ------ | --------------------------------------- | ------------------------------------------------- | ------- | ------------- |
| GET    | `/api/v1`                               | Discovery document (API metadata + HATEOAS links) | 200     | 500           |
| GET    | `/api/v1/rooms`                         | List all rooms                                    | 200     | 500           |
| POST   | `/api/v1/rooms`                         | Create a room                                     | 201     | 400, 409, 415 |
| GET    | `/api/v1/rooms/{roomId}`                | Fetch a single room                               | 200     | 404           |
| DELETE | `/api/v1/rooms/{roomId}`                | Delete a room (blocked if sensors exist)          | 204     | 409           |
| GET    | `/api/v1/sensors`                       | List sensors. Optional `?type=CO2`                | 200     | 500           |
| POST   | `/api/v1/sensors`                       | Register a sensor, validates `roomId`             | 201     | 400, 415, 422 |
| GET    | `/api/v1/sensors/{sensorId}`            | Fetch a single sensor                             | 200     | 404           |
| GET    | `/api/v1/sensors/{sensorId}/readings`   | Historical readings for the sensor                | 200     | 404           |
| POST   | `/api/v1/sensors/{sensorId}/readings`   | Append a new reading, updates `currentValue`      | 201     | 403, 404, 415 |

---

## 5. Sample curl Commands

Pre-populated on startup: rooms LIB-301, LAB-112, HALL-A and sensors TEMP-001, CO2-014, OCC-207 (in MAINTENANCE).

### 5.1 Discovery

```bash
curl http://localhost:8080/api/v1/
```

### 5.2 List rooms

```bash
curl http://localhost:8080/api/v1/rooms
```

### 5.3 Create a room

```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"GYM-01","name":"Main Gym","capacity":120}'
```

### 5.4 Try to delete a room that still has sensors (409 Conflict)

```bash
curl -i -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

Expected response:

```json
{
  "error": "RoomNotEmpty",
  "status": 409,
  "roomId": "LIB-301",
  "sensorCount": 2,
  "message": "Room LIB-301 still has 2 sensor(s) assigned.",
  "hint": "Remove or reassign the sensors before deleting this room."
}
```

### 5.5 Filter sensors by type (query parameter)

```bash
curl "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 5.6 Register a sensor with a bad roomId (422 Unprocessable Entity)

```bash
curl -i -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"BAD-01","type":"Humidity","roomId":"DOES-NOT-EXIST"}'
```

### 5.7 Append a reading to an ACTIVE sensor (side effect on currentValue)

```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":22.8}'

# then check the parent sensor to confirm the update
curl http://localhost:8080/api/v1/sensors/TEMP-001
```

### 5.8 Try to append a reading to a MAINTENANCE sensor (403 Forbidden)

```bash
curl -i -X POST http://localhost:8080/api/v1/sensors/OCC-207/readings \
  -H "Content-Type: application/json" \
  -d '{"value":1}'
```

---

## 6. Project Structure

```
smart-campus-api/
├── pom.xml
├── README.md
└── src/main/java/com/smartcampus/
    ├── Main.java                         entry point, Grizzly bootstrap
    ├── SmartCampusApplication.java       @ApplicationPath("/api/v1")
    ├── model/                            Room, Sensor, SensorReading POJOs
    ├── storage/                          DataStore singleton (ConcurrentHashMap)
    ├── resource/                         JAX-RS resource classes
    ├── exception/                        domain exceptions
    └── mapper/                           ExceptionMapper providers (409, 422, 403, 500)
```

---

## 7. Report: Answers to the Coursework Questions

### 7.1 JAX-RS Resource Lifecycle (Part 1.1)

By default JAX-RS treats every resource class as per-request. A fresh instance gets spun up for each incoming HTTP request, the framework injects the required context fields, the handler method runs, and the object is dropped at the end. It's not a singleton unless you annotate it with `@Singleton` or register it explicitly through an `Application` subclass as an instance rather than a class.

This matters a lot for how you handle shared state. Because each request gets its own object, any instance fields on the resource are effectively scoped to that single request. They don't survive. So if you try to keep your "database" as an instance field, it'll vanish between requests. That's why in this project the `DataStore` is a singleton sitting outside the resource classes, and each resource just grabs a reference to it at construction.

The real hazard is concurrency. Jersey runs handlers on a thread pool so multiple requests hit the shared `DataStore` at the same time. A plain `HashMap` would break under load. It could drop entries during resize, throw `ConcurrentModificationException` mid-iteration, or silently corrupt the structure. To stop that I used `ConcurrentHashMap` for the room and sensor maps, which handles concurrent puts and reads without external locks. For the per-sensor reading lists I went a step further and put a small `synchronized` block around each list so two threads can't append at the exact same instant and clobber each other. If I had skipped these guards the service would pass single-user testing and then fall apart the moment two facilities managers hit it together.

### 7.2 HATEOAS and Hypermedia (Part 1.2)

HATEOAS, which stands for Hypermedia As The Engine Of Application State, is the rule that says responses should include links telling the client where it can go next. The discovery endpoint in this project does that: it returns URLs for the rooms and sensors collections instead of forcing the client to know them in advance.

The benefit for a client developer is that the API becomes partly self-describing. Instead of hardcoding paths like `/api/v1/rooms` everywhere, the client reads them from the discovery document and follows them. If the server later moves rooms to a different path, or introduces a newer version, the client picks it up without a rewrite. That's harder to achieve with static PDF documentation, which goes stale the moment anyone refactors the URL scheme.

It also reduces the mental load on the client. You don't need to memorize the resource layout, you just follow links the same way you follow a link in a web page. Browsers do this with HTML and nobody thinks of it as complicated. Machine clients can do the same thing if the API gives them the links. Honestly in small projects it's overkill, but for a system with thousands of rooms and sensors and multiple consuming teams (facilities, BMS, mobile app) the saving adds up fast because the server can change the shape of its URLs without breaking every client at once.

### 7.3 Returning IDs vs Full Room Objects (Part 2.1)

Returning only IDs makes the response tiny. A list of a thousand rooms becomes a list of a thousand short strings, which transfers over the network in almost no time and costs the client very little to parse. The downside is every item in that list triggers a follow-up request to fetch the actual data, which hammers the server with N extra round trips. The "N+1 problem" lives here.

Returning full room objects is the opposite. One call, full payload, no follow-ups. But the response is bigger, possibly much bigger, and if the client only needed three rooms out of a thousand you wasted bandwidth on 997 rooms of data nobody read. On a mobile network that adds up to real cost and battery.

In practice the right answer usually sits in the middle. You return a slimmed-down summary: the id, the display name, the capacity, maybe a count of sensors, and a link to the full representation. Clients that need more call the detail endpoint. This is what the `GET /rooms` in this project does, it returns the full Room object because the payload is modest, but in a production system with thousands of rooms I'd swap it for a projection. The test that decides it is: how often will clients need each field, and how big does the list get.

### 7.4 DELETE Idempotency (Part 2.2)

My DELETE implementation is idempotent, yes. If a client sends the same `DELETE /rooms/GYM-01` request five times in a row, the first call removes the room and returns 204. Calls two through five hit a path where the room is no longer there, and the handler returns 204 again without touching any state. The observable outcome after the fifth call is identical to the outcome after the first: the room is gone. That's the definition of idempotence.

Compare that to a non-idempotent version that would return 404 on the second call. It would still be "safe" in the sense that repeated calls don't corrupt anything, but it would leak state information to the client, saying "this used to exist and now it doesn't." Some teams prefer that because it surfaces bugs. I went the other way because the REST spec recommends idempotent DELETE and because retry-heavy clients (think flaky mobile connections or a Postman collection re-run) shouldn't see different responses depending on network luck.

There's one nuance. The 409 branch, where the room still has sensors, is not idempotent in the strict sense because the response differs from the eventual 204 after someone removes the sensors. But that's a separate situation and it doesn't break idempotence for the pure "item is already absent" case, which is what the spec is really asking about.

### 7.5 @Consumes Mismatch Behaviour (Part 3.1)

Annotating a method with `@Consumes(MediaType.APPLICATION_JSON)` tells Jersey to only route requests here when the client sends a `Content-Type` header of `application/json` (or something compatible like `application/json; charset=utf-8`). If a client sends `text/plain` or `application/xml` instead, the framework checks the annotation, sees the mismatch, and refuses the request before the method body even runs.

The response the client gets back is HTTP 415 Unsupported Media Type. Jersey generates this automatically, I don't have to write any code to handle the mismatch. The method never gets called so there is no chance of a null pointer or a bad cast partway through processing.

I tested this with `curl -H "Content-Type: text/plain" -d 'bad' ...` and it returned 415 as expected. The benefit is clean input validation at the framework layer. If the API later adds XML support I just add `MediaType.APPLICATION_XML` to the annotation and Jersey will start accepting both. No changes to the handler logic, no if-else chains. This kind of declarative routing is one of the main wins of JAX-RS over raw servlets where you would have had to parse the header yourself.

### 7.6 @QueryParam vs Path Parameter for Filtering (Part 3.2)

The filter in this project is `GET /sensors?type=CO2`. The alternative design would have been `GET /sensors/type/CO2`. Both work, but the query parameter version is better for a few reasons.

First, query parameters compose naturally. If I want to add a second filter, say sensors in a specific room, the query string version becomes `?type=CO2&roomId=LIB-301` and keeps working. The path version would have to become something clunky like `/sensors/type/CO2/room/LIB-301`, and the order of the segments starts to matter, which it shouldn't.

Second, the query parameter is optional by design. Leave it off and you get the full list. With a path segment the framework has to special-case the missing-filter case because `/sensors/type/` is not the same URL as `/sensors`. That's an awkward shape for a REST API.

Third, query parameters tell the reader what they are. `?type=CO2` is self-documenting. `/type/CO2/` might mean filtering, or it might mean drilling into a "type" sub-resource. Keeping filtering in the query string and resource identity in the path preserves the mental model that URLs are nouns. Filters are adjectives.

There's one case where the path segment wins. If `type=CO2` were a truly distinct resource with its own endpoints (rare in this domain) then modelling it as `/sensor-types/CO2/sensors` would be fair. But for a simple optional filter the query parameter is the cleaner choice, and that's what the JAX-RS designers had in mind when they gave us `@QueryParam` separate from `@PathParam`.

### 7.7 Sub-Resource Locator Pattern (Part 4.1)

The sub-resource locator lives on `SensorResource` and looks like:

```java
@Path("/{sensorId}/readings")
public SensorReadingResource readings(@PathParam("sensorId") String sensorId) {
    ...
    return new SensorReadingResource(sensorId);
}
```

Instead of putting `GET /sensors/{id}/readings`, `POST /sensors/{id}/readings`, `DELETE /sensors/{id}/readings/{rid}` and every future readings operation inside `SensorResource`, the locator hands off to a dedicated `SensorReadingResource`. The method doesn't have a verb annotation like `@GET` or `@POST`. That's how Jersey recognises it as a locator.

The architectural gain here is separation of concerns. `SensorResource` is about sensor CRUD. `SensorReadingResource` is about time-series data. If I kept everything in one class I'd have a growing pile of methods, each with a longer `@Path`, and the class would slowly become the "god object" of the API. Splitting it keeps each class small enough to hold in your head, easier to test in isolation, and easier to hand to another developer who only needs to work on readings.

It also lets each sub-resource carry its own context. The `SensorReadingResource` holds the `sensorId` as a final field after construction, so every method in it already knows which sensor it belongs to. No repeated `@PathParam` lookup at the start of every handler. The code reads cleaner.

The pattern scales. Add rooms alerts? Add `/rooms/{id}/alerts` as a locator on `SensorRoomResource` that returns an `AlertResource`. Add per-sensor calibration settings? Another locator, another focused class. You avoid the single-controller megastructure that Spring developers sometimes end up with when they nest too many `@RequestMapping` paths in one file.

### 7.8 Why HTTP 422 Instead of 404 (Part 5.2)

404 Not Found is for when the URL itself points at nothing. If a client does `GET /sensors/UNKNOWN-999`, that request is asking for a sensor that doesn't exist, and 404 is exactly right. The URL is the thing being denied.

422 Unprocessable Entity is different. The URL is fine, the body is syntactically valid JSON, and the request would normally succeed. But a value inside the body refers to something that doesn't exist. In this project, if a client POSTs a new sensor with `"roomId":"NOPE-999"` to `/sensors`, the URL is correct, the JSON parses cleanly, but the business rule says the room must exist first. Returning 404 here would confuse the client. They would think `/sensors` itself is missing, which it isn't. They'd probably go hunting for a typo in the URL.

422 says "I understood the request, I can process this kind of thing, but the data you gave me is semantically wrong in a way I can't fix." That's a much more actionable message. The client developer now knows: check the fields inside the payload, not the URL. Some teams prefer 400 Bad Request for this, which is acceptable, but 400 is a catch-all for anything malformed. 422 is the narrower, more precise answer for a "valid shape, invalid reference" failure. Clients that parse error responses programmatically can react differently to 422 (fix a reference, try again) than to 400 (body is structurally broken, bigger problem).

### 7.9 Security Risks of Leaking Stack Traces (Part 5.4)

Exposing a Java stack trace to an external consumer is a gift to an attacker. Several specific risks come out of it.

First, the trace usually includes the fully-qualified class names of your own code. An attacker reads `com.smartcampus.resource.SensorResource` and immediately learns your package structure, your framework of choice (the `org.glassfish.jersey` frames give Jersey away), and which methods are in play. That's reconnaissance they didn't have to do themselves.

Second, the trace often reveals library versions. The shaded jar ends up with frame numbers like `GrizzlyHttpServerFactory.java:316` that can be tied to a specific Grizzly release. An attacker cross-references that against the CVE database and finds any known vulnerabilities tied to that version.

Third, stack traces leak deployment details. Some traces include absolute filesystem paths like `/opt/smartcampus/lib/...` which tell the attacker you're on a Unix host, which user account is running the service, and whether the app is in a standard location. That shapes their next move.

Fourth, and most dangerous, certain exceptions contain data values in their message. A `SQLException` might echo back a partial SQL query. A `NullPointerException` from a misconfigured library might reveal a config key. Even an innocent `IndexOutOfBoundsException` that includes the index and list size tells the attacker about internal array sizing. In some cases this bleeds into actual PII or credentials.

That's why the catch-all `GenericExceptionMapper` in this project logs the full trace server-side with a generated UUID as the trace id, then returns only the generic message and the UUID to the client. The admin can look up the UUID in the logs to find the real cause. The client gets no free intelligence. It's a small amount of extra code but it shuts down a whole category of information disclosure.

---

## 8. Video Demonstration

The 10-minute Postman walkthrough is available at: ``

---

## 9. Licence

This project was submitted as coursework for the University of Westminster's 5COSC022W Client-Server Architectures module. Not intended for production use.
